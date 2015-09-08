# HttpInvoke
httpclient 封装--HttpClient4.3

释放连接
1.必须关闭调用CloseableHttpResponse#close()方法
2.保证输入流被完全消费 否则会被连接管理器关闭丢弃

连接管理器如何判断流是否消费完？？？
测试消费部分流 然后关闭掉 

请求响应的实体内容只有一小部分需要被检索，剩下的都不需要，而消费剩下的内容又有性能损耗，
造成重用连接很高。这种情况下，可以直接关闭response来终止内容流，废弃连接！

Chunk encoded POST


CloseableHttpClient httpclient = HttpClients.createDefault();
HttpGet httpGet = new HttpGet("http://targethost/homepage");
CloseableHttpResponse response1 = httpclient.execute(httpGet);
// The underlying HTTP connection is still held by the response object
// to allow the response content to be streamed directly from the network socket.
// In order to ensure correct deallocation of system resources
// the user MUST call CloseableHttpResponse#close() from a finally clause.
// Please note that if response content is not fully consumed the underlying
// connection cannot be safely re-used and will be shut down and discarded
// by the connection manager. 
try {
    System.out.println(response1.getStatusLine());
    HttpEntity entity1 = response1.getEntity();
    // do something useful with the response body
    // and ensure it is fully consumed
    EntityUtils.consume(entity1);
} finally {
    response1.close();
}

HttpPost httpPost = new HttpPost("http://targethost/login");
List <NameValuePair> nvps = new ArrayList <NameValuePair>();
nvps.add(new BasicNameValuePair("username", "vip"));
nvps.add(new BasicNameValuePair("password", "secret"));
httpPost.setEntity(new UrlEncodedFormEntity(nvps));
CloseableHttpResponse response2 = httpclient.execute(httpPost);

try {
    System.out.println(response2.getStatusLine());
    HttpEntity entity2 = response2.getEntity();
    // do something useful with the response body
    // and ensure it is fully consumed
    EntityUtils.consume(entity2);
} finally {
    response2.close();
}
