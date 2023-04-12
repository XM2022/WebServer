package priv.xm.xmserver.xmLibrary1_0;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;

/**
 * 一个包含实用的静态方法的工具类.
 * @author XM
 *
 */
@SuppressWarnings("rawtypes")
final public class Util {
    /**
     * 一个通用的toString()方法,适用于任何类.
     * 注意:
     * 在打印所有成员的情况下, 
     * 1.可能因访问私有成员被模块安全管理器拒绝而抛出异常!
     * 2.可能因未来Java版本中可能因反射机制对私有权限的访问屏蔽改动而失效抛异常!
     * @param obj 任意类型的Class对象
     * @param all 只接受0或1个参数
     *                   0个参数或true默认打印所有成员--通用性较差.
     *                   false打印公共成员--更强的通用性,不会因Java版本改动而失效.
     */
    public static String toString(Object obj, boolean... all) {
        if(obj == null) return "null"; 
        if(all==null || all.length>1) throw new IllegalArgumentException();
        Class cls = obj.getClass();
        if (cls.isPrimitive() || isBoxedPrimitive(cls) || cls == String.class) {
            return obj.toString();
        }
        
        String str = "";
        if(cls.isArray()) {
            Class elementType = cls.getComponentType();
            str += getSimpleName(cls);
            str += "[";
            for (int i = 0; i < Array.getLength(obj); i++) {
                if(i > 0) str += ", ";
                Object e = Array.get(obj, i);
                if(elementType.isPrimitive()) str += e;
                else if(elementType == String.class) str += "\"" + e +"\"";
                else str += toString(e, all); //递归遍历数组内部成员
            }
            str += "]";
        }
        else {  //对象类型;不支持装箱类型和String(请直接打印其值),因为其某些成员可能循环调用而导致堆栈溢出.
            String bracketL = "\n{\n";
            str += getSimpleName(cls) + bracketL;
            Field[] fields = null;
            fields =(all.length==0 || all[0]==true) ? cls.getDeclaredFields() : cls.getFields();
            AccessibleObject.setAccessible(fields, true);  //将所有成员设为可读写状态,未来Java版本可能失效.
            for (var e : fields) {
                if(!str.endsWith(bracketL)) str += "\n";
                String modifier = Modifier.toString(e.getModifiers());
                str += modifier.length()!=0 ? "\t" + modifier + " " : "\t";
                Class type = e.getType();
                str += String.format("%s %s = ", getSimpleName(type), e.getName());
                try{
                    str += e.get(obj) + ";";  //不作深入递归,彻底解决类成员循环引用、标准库模块安全管理器拒绝访问等问题.
                } catch(IllegalAccessException iae) {
                    iae.printStackTrace();
                }
            }
            str += "\n}";
        }
        return str;
    }
    
    /**
     * @param type 类、对象数组和原生数组类型(一至多维)
     * @return
     */
    private static String getSimpleName(Class type) {
        String name = type.getName();
        int dimension = getDimension(type);
        String simpleName = null;
        if(dimension == 0) { //类
            simpleName = name.substring(name.lastIndexOf(".")+1);  
        }
        else { //数组
            Class eleType = type;
            for (; dimension-- > 0;) { //获取元素类型
                eleType = eleType.getComponentType();
            }
            String eleName = eleType.getName();
            simpleName = eleName.substring(eleName.lastIndexOf('.')+1).replace(";", "");
        }
        return simpleName;
    }
    
    final static private List<Class> boxedTypes = Arrays.<Class>asList(Boolean.class, Character.class, Byte.class
            , Short.class, Integer.class, Long.class, Float.class, Double.class);
    
    public static boolean isBoxedPrimitive(Class cls) {
        return boxedTypes.contains(cls);
    }
    
    /**
     * 一个通用的获取数组维数的方法．
     * @param array 任意类型
     * @return 若非数组,返回0.
     */
    public static int getDimension(Class array) { 
        Class eleType = array.getComponentType();
        int dimension = 0;
        while(eleType != null){
            ++dimension;
            eleType = eleType.getComponentType();
        }
        return dimension;
    }
    
    /**
     * 一个通用的比较相等方法.(--可能不适用于集合)
     * 此方法基本实现了任何类型的equals()方法,
     * 但使用场景局限性很大:
     * 仅能用于继承链中最底层的那一个还未实现equals()方法的子类,
     * 且要求继承链中的所有父类已正确覆盖了equals()方法.
     * 另外,不同于通用toString(),此方法不能用于子类的覆盖equals()方法体中,
     * 否则将引发jInvocationTargetException异常 //对于实现1:if(!(Boolean)equalsMethod.invoke(obj1, obj2)) return false; 
     *     或堆栈溢出. //if(!Objects.equals(obj1, obj2)) return false;
     * 1.只支持同一种类之间比较(getClass),不支持父子类比较(instanceof),
     *   因为这是一个通用的静态方法,无法保证实参传入的父子类的equals()方法
     *   具有相同的语义(final equals()方法不被子类覆盖),
     *   使用instanceof将违反对称性原则.
     * 2.运用反射机制,所有可能调用失败.
     * @param obj1 任意类型, 除了数组类型--难以实现
     * @param obj2 任意类型, 除了数组类型--难以实现
     * @return 父子类返回false
     */
    public static boolean equals(Object obj1, Object obj2) {
        if(obj1==obj2) return true;  //同为null或同一对象
        if(obj1 == null || obj2 == null) return false;
        
        Class<? extends Object> obj1Class = obj1.getClass();
        Class<? extends Object> obj2Class = obj2.getClass();
        if(obj1Class.isArray() || obj2Class.isArray()){
            throw new IllegalArgumentException("不能传入数组!");
        }

        if(obj1Class == obj2Class) { //若父子类,则不相等
            if(obj1Class==String.class || Util.isBoxedPrimitive(obj1Class)) { //对象自身是String或装箱类型--不应该深入去比较其成员
                return obj1.equals(obj2);  //Objects.equals()
            }
            //应该先调用super.equals()判断父类成员是否相等,
            //否则无论getDeclaredFields()还是getFields()均造成部分父/子类的公/私有成员部分未参与比较.
            try {
                Class<?> superClass = obj1Class.getSuperclass();
                Method equalsMethod = superClass.getMethod("equals", Object.class);
                equalsMethod.setAccessible(true);
                if(!(Boolean)equalsMethod.invoke(obj1, obj2)) return false;  //验证真的会调用父类equals()吗?
                //--实测不行!仍调用的是子类方法. 不过调用此方法的类equals()一般未实现.
                
                //if(!Objects.equals(obj1, obj2)) return false; //等价于上面一大段--子类未覆盖,将调用父类equals().
            } catch(ReflectiveOperationException roe) {
                System.err.println("这些异常不可能发生!");
                roe.printStackTrace(System.out);
            }
            
            Field[] obj1Fields = obj1Class.getDeclaredFields(),
                    obj2Fields = obj2Class.getDeclaredFields();
            Field.setAccessible(obj1Fields, true);
            Field.setAccessible(obj2Fields, true);
            for (int i = 0; i < obj1Fields.length; i++) {
                Field field1 = obj1Fields[i];
                Field field2 = obj2Fields[i];
                Class<?> field1Type = field1.getType();
                //只用判断filed1类型,field2类型与其一致, 因为它们属于是同一种类
                if(field1Type == String.class || Util.isBoxedPrimitive(field1Type)) {  //String和装箱类型使用覆盖的equals()
                    try {
                        Object field1Value = field1.get(obj1);
                        Object field2Value = field2.get(obj2);
                        if(!field1Value.equals(field2Value)) return false;
                    }catch(IllegalAccessException iae) {
                        System.err.println("模块安全管理器拒绝访问!");
                        iae.printStackTrace();
                    }
                }
                else if(field1Type.isPrimitive()){
                    if(field1 != field2) return false;
                }
                else {  //对象类型
                    if(!Objects.equals(obj1, obj2)) return false; 
                }
            } //for
            return true;
        } //if(obj1Class == obj2Class)
        return false;
    }
    
    /**Linux、Window下均可正确获得本机IP, 不管是否开启虚拟机网卡*/
    public static String getRealLocalAddress() {
        try {
            Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (allNetInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = (NetworkInterface) allNetInterfaces
                        .nextElement();
 
                // 去除回环接口，子接口，未运行和接口
                if (netInterface.isLoopback() || netInterface.isVirtual()
                        || !netInterface.isUp()) {
                    continue;
                }
                
                if (!netInterface.getDisplayName().contains("Intel")
                        && !netInterface.getDisplayName().contains("Realtek")) {
                    continue;
                }
                Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress ip = addresses.nextElement();
                    if (ip != null) {
                        // ipv4
                        if (ip instanceof Inet4Address) {
                            return ip.getHostAddress();
                        }
                    }
                }
                break;
            }
        } catch (SocketException e) {
            System.err.println("Error when getting host ip address"
                    + e.getMessage());
        }
        return null;
    }
    
}
