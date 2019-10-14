package nicelee.function.danmuku;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import nicelee.common.annotation.Autoload;
import nicelee.common.util.PackageScanLoader;
import nicelee.function.danmuku.core.IDanmuku;

public class DanmukuManager {

	static HashMap<String, Class<?>> iDanmukuMap;
	static {
		iDanmukuMap = new HashMap<>();
		// 扫描包，加载 含有注解Autoload 的 IDanmuku 的实现类
		PackageScanLoader pLoader = new PackageScanLoader() {
			@Override
			public boolean isValid(Class<?> klass) {
				Autoload load = klass.getAnnotation(Autoload.class);
				if (null != load && IDanmuku.class.isAssignableFrom(klass)) {
					iDanmukuMap.put(load.source(), klass);
				}
				return false;
			}
		};
		pLoader.scanRoot("nicelee.function.danmuku.core.impl");
	}
	
	/**
	 * 根据参数获取实现类
	 * @param source
	 * @param roomId
	 * @return
	 */
	public static IDanmuku createDanmuku(String source, long roomId) {
		Class<?> klass = iDanmukuMap.get(source);
		try {
			Method method = klass.getMethod("create", long.class);
			return (IDanmuku)method.invoke(null, roomId);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
			return null;
		}
	}
}
