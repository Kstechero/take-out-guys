package com.sky.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sky.properties.AgentServiceProperties;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.BiConsumer;

@Component
public class AgentServiceHttp {
    private final AgentServiceProperties properties;
    private final ObjectMapper mapper;
    private final RestTemplate restTemplate;

    public AgentServiceHttp(AgentServiceProperties properties, ObjectMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeout());
        factory.setReadTimeout(properties.getReadTimeout());
        this.restTemplate = new RestTemplate(factory);
    }

    public Result userChat(String requestId, Long actorId, Long sessionId, String message) {
        return chat("/v1/user/chat", requestId, "user", actorId, "USER", sessionId, message);
    }
    public Result adminChat(String requestId, Long actorId, Long sessionId, String message) {
        return chat("/v1/admin/chat", requestId, "admin", actorId, "ADMIN", sessionId, message);
    }
    public Map<String,Object> recommend(String requestId, Long actorId, String requirement,
                                        BigDecimal budget, Integer peopleCount) {
        Map<String,Object> payload=new LinkedHashMap<>();
        payload.put("request_id",requestId); payload.put("actor",actor("user",actorId,"USER"));
        payload.put("requirement",requirement); payload.put("budget",budget);
        payload.put("people_count",peopleCount==null?1:peopleCount); payload.put("limit",5);
        return postMap("/v1/user/recommendations",payload);
    }
    public Map<String,Object> reviewDraft(String requestId, Long actorId, Long orderId, Long dishId,
                                          Integer rating, String highlights, String style) {
        Map<String,Object> payload=new LinkedHashMap<>();
        payload.put("request_id",requestId); payload.put("actor",actor("user",actorId,"USER"));
        payload.put("order_id",orderId); payload.put("dish_id",dishId);
        payload.put("rating",rating==null?5:rating); payload.put("highlights",highlights==null?"":highlights);
        payload.put("style",style==null?"自然":style);
        return postMap("/v1/user/reviews/draft",payload);
    }
    public Result resumeUser(String requestId, Long actorId, Long sessionId,
                             String confirmationToken, String decision, Object editedArguments) {
        return resume("user_support_agent", requestId, "user", actorId, "USER",
                sessionId, confirmationToken, decision, editedArguments);
    }
    public Result resumeAdmin(String requestId, Long actorId, Long sessionId,
                              String confirmationToken, String decision, Object editedArguments) {
        return resume("admin_operations_agent", requestId, "admin", actorId, "ADMIN",
                sessionId, confirmationToken, decision, editedArguments);
    }
    private Result resume(String agentName, String requestId, String type, Long actorId, String role,
                          Long sessionId, String confirmationToken, String decision, Object editedArguments) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("request_id", requestId);
        payload.put("agent_name", agentName);
        payload.put("actor", actor(type, actorId, role));
        payload.put("confirmation_token", confirmationToken);
        payload.put("decision", decision);
        payload.put("edited_arguments", editedArguments);
        return post("/v1/threads/" + sessionId + "/resume", payload);
    }
    public Result userStream(String requestId, Long actorId, Long sessionId, String message, BiConsumer<String,Object> events) {
        return stream("/v1/user/chat/stream", requestId, "user", actorId, "USER", sessionId, message, events);
    }
    public Result adminStream(String requestId, Long actorId, Long sessionId, String message, BiConsumer<String,Object> events) {
        return stream("/v1/admin/chat/stream", requestId, "admin", actorId, "ADMIN", sessionId, message, events);
    }
    public Map<String,Object> health() {
        try { return mapper.convertValue(mapper.readTree(restTemplate.getForObject(endpoint("/health"), String.class)), Map.class); }
        catch (Exception ex) { return new LinkedHashMap<String,Object>() {{ put("status","DOWN"); put("service","agent-api"); put("message",ex.getMessage()); }}; }
    }
    private Result chat(String path,String requestId,String type,Long id,String role,Long sessionId,String message) {
        return post(path, body(requestId, type, id, role, sessionId, message));
    }
    private Result post(String path, Map<String, Object> body) {
        try {
            ResponseEntity<String> response=restTemplate.exchange(endpoint(path),HttpMethod.POST,new HttpEntity<>(body,headers()),String.class);
            JsonNode p=mapper.readTree(response.getBody()); String answer=text(p,"answer");
            if(answer==null||answer.trim().isEmpty()) throw new IllegalStateException("Agent API returned an empty answer");
            Map<String,Object> confirmation=objectMap(p.get("confirmation"));
            String status=text(p,"status");
            if(status==null) status=confirmation==null?"completed":"waiting_user";
            return new Result(text(p,"session_id"),answer,text(p,"trace_id"),status,confirmation);
        } catch(Exception ex){ throw new IllegalStateException("Agent API request failed",ex); }
    }
    private Map<String,Object> postMap(String path, Map<String,Object> body) {
        try {
            ResponseEntity<String> response=restTemplate.exchange(endpoint(path),HttpMethod.POST,
                    new HttpEntity<>(body,headers()),String.class);
            return mapper.convertValue(mapper.readTree(response.getBody()),Map.class);
        } catch(Exception ex){ throw new IllegalStateException("Agent API request failed",ex); }
    }
    private Result stream(String path,String requestId,String type,Long id,String role,Long sessionId,String message,BiConsumer<String,Object> events) {
        StringBuilder answer=new StringBuilder(); String[] session={sessionId==null?null:String.valueOf(sessionId)}; String[] trace={null}; String[] status={null}; Map<String,Object>[] confirmation=new Map[]{null};
        try {
            restTemplate.execute(endpoint(path),HttpMethod.POST,request->{request.getHeaders().putAll(headers());mapper.writeValue(request.getBody(),body(requestId,type,id,role,sessionId,message));},response->{
                try(BufferedReader reader=new BufferedReader(new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))){
                    String event="message",line; StringBuilder data=new StringBuilder();
                    while((line=reader.readLine())!=null){
                        if(line.startsWith("event:"))event=line.substring(6).trim();
                        else if(line.startsWith("data:")){if(data.length()>0)data.append('\n');data.append(line.substring(5).trim());}
                        else if(line.isEmpty()&&data.length()>0){consume(event,data.toString(),answer,session,trace,status,confirmation,events);event="message";data.setLength(0);}
                    }
                    if(data.length()>0)consume(event,data.toString(),answer,session,trace,status,confirmation,events);
                } return null;
            });
            String finalStatus=status[0]==null?(confirmation[0]==null?"completed":"waiting_user"):status[0];
            return new Result(session[0],answer.toString(),trace[0],finalStatus,confirmation[0]);
        } catch(Exception ex){throw new IllegalStateException("Agent API stream request failed",ex);}
    }
    private void consume(String event,String data,StringBuilder answer,String[] session,String[] trace,String[] status,Map<String,Object>[] confirmation,BiConsumer<String,Object> events)throws java.io.IOException{
        JsonNode p=mapper.readTree(data); if("delta".equals(event)&&text(p,"text")!=null)answer.append(text(p,"text"));
        if("confirmation".equals(event)){
            confirmation[0]=objectMap(p);
            String summary=text(p,"summary");
            if(summary!=null&&answer.length()==0)answer.append(summary);
        }
        if("done".equals(event)){session[0]=text(p,"session_id");trace[0]=text(p,"trace_id");status[0]=text(p,"status");} events.accept(event,mapper.convertValue(p,Object.class));
    }
    private Map<String,Object> body(String requestId,String type,Long id,String role,Long sessionId,String message){
        Map<String,Object> body=new LinkedHashMap<>();body.put("request_id",requestId);body.put("actor",actor(type,id,role));body.put("session_id",sessionId==null?null:String.valueOf(sessionId));body.put("message",message);body.put("confirmed_action_token",null);return body;
    }
    private Map<String,Object> actor(String type,Long id,String role){Map<String,Object> actor=new LinkedHashMap<>();actor.put("type",type);actor.put("id",String.valueOf(id));actor.put("roles",Collections.singletonList(role));actor.put("expires_at",Instant.now().plus(5,ChronoUnit.MINUTES).toString());return actor;}
    private HttpHeaders headers(){HttpHeaders h=new HttpHeaders();h.setContentType(MediaType.APPLICATION_JSON);h.setAccept(Arrays.asList(MediaType.APPLICATION_JSON,MediaType.TEXT_EVENT_STREAM));if(properties.getAuthToken()!=null&&!properties.getAuthToken().trim().isEmpty())h.set("X-Agent-Service-Token",properties.getAuthToken());return h;}
    private String endpoint(String path){return properties.getBaseUrl().replaceAll("/+$","")+path;}
    private String text(JsonNode p,String field){JsonNode v=p==null?null:p.get(field);return v==null||v.isNull()?null:v.asText();}
    private Map<String,Object> objectMap(JsonNode value){return value==null||value.isNull()||!value.isObject()?null:mapper.convertValue(value,Map.class);}
    public static class Result{
        private final String sessionId,answer,traceId,status;
        private final Map<String,Object> confirmation;
        public Result(String sessionId,String answer,String traceId){this(sessionId,answer,traceId,"completed",null);}
        public Result(String sessionId,String answer,String traceId,String status,Map<String,Object> confirmation){this.sessionId=sessionId;this.answer=answer;this.traceId=traceId;this.status=status;this.confirmation=confirmation;}
        public String getSessionId(){return sessionId;}public String getAnswer(){return answer;}public String getTraceId(){return traceId;}public String getStatus(){return status;}public Map<String,Object> getConfirmation(){return confirmation;}
    }
}
