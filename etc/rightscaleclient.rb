#!/usr/bin/env ruby 
require File.expand_path(File.dirname(__FILE__) + '/kerryb-right_http_connection-1.2.5/lib/right_http_connection')
require File.expand_path(File.dirname(__FILE__) + '/right_aws-1.10.0/lib/right_aws')

ENV['S3_URL'] = "http://localhost:9090"

access_id = ARGV.delete_at(0)
secret_key = ARGV.delete_at(0)

command = ARGV.delete_at(0)
params = ""

##
## yes, I know this is horrible!!
##
ARGV.each do|a|
	if a.include?("=>")
		params = params + ", " + a
	elsif a.include?("File.open")
		params = params + ", " + a
	else
		params = params + ", '" + a + "'"
	end
end
puts params

s3 = RightAws::S3Interface.new(access_id, secret_key)

evalstr = "s3.send('" + command + "'" + params + ")"
puts evalstr
result = eval(evalstr)

p result
