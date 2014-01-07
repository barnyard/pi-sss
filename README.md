# pi-sss
## Simple Storage Service (S3)


## Build

This project depends upon `p2p-build` being setup with `ant` and `ant-contrib`. [See instructions](http://barnyard.github.io/p2p-build)

### Dependencies

The following projects should be siblings to this project and had 'publish' targets run on them.

- [p2p-app](http://barnyard.github.io/p2p-app)

### Compiling

Add a properties file with your configuration you'd like in the `properties` directory.

    ant

### Testing

To get this projects integration tests running you will need to do this:

1. apt-get install libopenssl-ruby
2. gem install builder
3. gem install mime-types
4. gem install xml-simple
5. make all .rb files in etc executable
6. make etc/s3curl executable
7. sudo cpan Digest::HMAC

