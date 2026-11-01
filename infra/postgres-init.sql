\getenv app_password BEHAVIORGUARD_DB_PASSWORD
CREATE ROLE behaviorguard_app LOGIN PASSWORD :'app_password';
ALTER DATABASE behaviorguard OWNER TO behaviorguard_app;