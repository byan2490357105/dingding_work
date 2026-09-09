import io.jsonwebtoken.*;

import java.util.UUID;

public class Test {
    private long daytoms=1000*60*60*24;
    private String signature="admin";

    @org.junit.jupiter.api.Test
    public void JWT(){
        JwtBuilder jwtBuilder=Jwts.builder();
        String jwtToekn=jwtBuilder
                .setHeaderParam("yyp","JWT")
                .setHeaderParam("alg","HS256")
                //payload
                .claim("username","tom")
                .claim("role","admin")
                .setSubject("admin-test")
                //token存活时间为一天
                .setExpiration(new java.util.Date(System.currentTimeMillis()+daytoms))
                .setId(UUID.randomUUID().toString())
                //签名
                .signWith(SignatureAlgorithm.HS256,signature)
                .compact();
        System.out.println(jwtToekn);
    }

    @org.junit.jupiter.api.Test
    public void parse(){
        String token="eyJ5eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VybmFtZSI6InRvbSIsInJvbGUiOiJhZG1pbiIsInN1YiI6ImFkbWluLXRlc3QiLCJleHAiOjE3ODg3NjA5MjMsImp0aSI6IjU4OGU4OWYyLTQ1M2MtNDQyMy04MmU3LTI2ZjhlYThkYmM1YyJ9.AfbloHAm2SUHi3WfdzChoqkm5krbJ9oB2zHEvx_XKsc";
        JwtParser jwtParser=Jwts.parser();
        Jws<Claims> claimsJwts=jwtParser.setSigningKey(signature).parseClaimsJws(token);
        Claims claims=claimsJwts.getBody();
        System.out.println(claims.get("username"));
        System.out.println(claims.get("role"));
        System.out.println(claims.getId());
        System.out.println(claims.getExpiration());
    }
}
