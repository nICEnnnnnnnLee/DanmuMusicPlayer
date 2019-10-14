package nicelee.function.danmuku.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nicelee.common.annotation.Autoload;
import nicelee.common.util.PackageScanLoader;

public class MsgHandler {

	static HashMap<String, List<IMsgHandler>> iMap;
	static {
		iMap = new HashMap<>();
		// 扫描包，分类加载 含有注解Autoload 的 IMsgHandler 的实现类
		PackageScanLoader pLoader = new PackageScanLoader() {
			@Override
			public boolean isValid(Class<?> klass) {
				Autoload load = klass.getAnnotation(Autoload.class);
				if (null != load && IMsgHandler.class.isAssignableFrom(klass)) {
					List<IMsgHandler> handlers = iMap.get(load.source());
					if(handlers == null) {
						handlers = new ArrayList<>();
						iMap.put(load.source(), handlers);
					}
					try {
						handlers.add((IMsgHandler) klass.newInstance());
					} catch (InstantiationException | IllegalAccessException e) {
						e.printStackTrace();
					}
				}
				return false;
			}
		};
		pLoader.scanRoot("nicelee.function.danmuku.handler.impl");
	}
	
	/**
	 * 根据参数获取实现类
	 * @param source
	 * @param roomId
	 * @return
	 */
	public static List<IMsgHandler> getHandlers(String source) {
		return iMap.get(source);
	}
}
