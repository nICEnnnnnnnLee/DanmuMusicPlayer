package nicelee.function.music.domain;

public class Music {

	public String id;
	public String name;
	public String singer;
	public String url;
	public String album;
	public String remark;
	
	public void print(){
//		java.lang.reflect.Field[] fields = this.getClass().getDeclaredFields();
//		System.out.println(" --------");
//		for(int i = 0 , len = fields.length; i < len; i++) {
//			// 对于每个属性，获取属性名
//			String varName = fields[i].getName();
//			try {
//				// 获取在对象f中属性fields[i]对应的对象中的变量
//				String value = (String)fields[i].get(this);
//				if( value != null && !value.equals("")){
//					System.out.printf(" ---%s的值为: %s\n" ,varName, value);
//				}
//			} catch (IllegalArgumentException ex) {
//				ex.printStackTrace();
//			} catch (IllegalAccessException e) {
//				e.printStackTrace();
//			}
//		}
//		System.out.println(" --------");
	}
}
