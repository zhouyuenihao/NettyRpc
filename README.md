# NettyRpc
An RPC framework based on Netty, ZooKeeper and Spring  
中文详情：[Chinese Details](http://www.cnblogs.com/luxiaoxun/p/5272384.html)
### Features:
* Simple code and framework
* Service registry/discovery support by ZooKeeper
* High availability, load balance and failover
* Asynchronous/synchronous call support
* Different serializer/deserializer support
* Dead TCP connection detecting with heartbeat
### Design:
![design](https://github.com/luxiaoxun/NettyRpc/blob/master/picture/NettyRpc-design.png)
### How to use (netty-rpc-test)
1. Define an interface:

	    public interface HelloService { 
			String hello(String name); 
			String hello(Person person);
		}

2. Implement the interface with annotation @RpcService:

		@RpcService(HelloService.class)
		public class HelloServiceImpl implements HelloService {
			public HelloServiceImpl(){}
			
			@Override
			public String hello(String name) {
				return "Hello! " + name;
			}

			@Override
			public String hello(Person person) {
				return "Hello! " + person.getFirstName() + " " + person.getLastName();
			}
		}

3. Run zookeeper

   For example: zookeeper is running on 127.0.0.1:2181

4. Start server:

   Start server with spring config: RpcServerBootstrap

   Start server without spring config: RpcServerBootstrap2

5. Use the client:

		final RpcClient rpcClient = new RpcClient("127.0.0.1:2181");
		
		// Sync call
		HelloService helloService = rpcClient.createService(HelloService.class);
		String result = helloService.hello("World");
		
		// Async call
		RpcService client = rpcClient.createAsyncService(HelloService.class);
		RPCFuture helloFuture = client.call("hello", "World");
		String result = (String) helloFuture.get(3000, TimeUnit.MILLISECONDS);

