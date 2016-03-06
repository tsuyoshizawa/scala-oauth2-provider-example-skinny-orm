# scala-oauth2-provider example with Skinny-ORM

- [scala-oauth2-provider](https://github.com/nulab/scala-oauth2-provider) 0.17.x
- [Play Framework](https://www.playframework.com/) 2.5.x
- [Skinny-ORM](http://skinny-framework.org/documentation/orm.html) 2.0.x

## Running Play Framework with evolutions

```
$ sbt -Dplay.evolutions.db.default.autoApply=true run
```

## Try to create access tokens using curl

### Client credentials

```
$ curl http://localhost:9000/oauth/access_token -X POST -d "client_id=bob_client_id" -d "client_secret=bob_client_secret" -d "grant_type=client_credentials"
```

### Authorization code

```
$ curl http://localhost:9000/oauth/access_token -X POST -d "client_id=alice_client_id" -d "client_secret=alice_client_secret" -d "redirect_uri=http://localhost:3000/callback" -d "code=bob_code" -d "grant_type=authorization_code"
```

NOTE: A service needs to generate `code` in advance. In this example, the code has been inserted in database by evolutions.

### Password

```
$ curl http://localhost:9000/oauth/access_token -X POST -d "client_id=alice_client_id2" -d "client_secret=alice_client_secret2" -d "username=alice@example.com" -d "password=alice" -d "grant_type=password"
```

### Refresh token

```
$ curl http://localhost:9000/oauth/access_token -X POST -d "client_id=alice_client_id2" -d "client_secret=alice_client_secret2" -d "refresh_token=${refresh_token}" -d "grant_type=refresh_token"
```

NOTE: `${refresh_token}` is you got `refresh_token` from json of password grant. (client_id and client_secret are also same with password grant)

### Access resource using access_token

You can access application resource using access token.

```
$ curl --dump-header - -H "Authorization: Bearer ${access_token}" http://localhost:9000/resources
```

In this example, server just returns authorized user information.

```
HTTP/1.1 200 OK
Content-Type: application/json; charset=utf-8
Content-Length: 90

{"account":{"email":"alice@example.com"},"clientId":"alice_client_id2","redirectUri":null}
```
