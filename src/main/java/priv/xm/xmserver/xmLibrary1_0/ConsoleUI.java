package priv.xm.xmserver.xmLibrary1_0;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import priv.xm.xmserver.xmLibrary1_0.exceptions.consoleUI.menu.MenuFunctionBindFailException;
import priv.xm.xmserver.xmLibrary1_0.interfaces.Function;


/**
 * 本类可以快速构建控制台程序交互界面. 控制台建议使用等宽字体(一个中文占2个英文), 否则将出现一些显示对齐小问题.
 * 注意需要依赖自定义异常类包!
 * 不支持动态添加菜单.后期如果改动此设计,需要作额外的动态测试.
 * 本类含有一些实用的控制台输入的静态方法, 可用于其它项目.
 * 
 * 想法: 1.增加"关于"和"帮助"
 * @author XM
 *
 *///因为实现了run()方法一键式启动运行,
 ///所以原本的public对外接口方法一律隐藏,均设置为protected.
public class ConsoleUI {
    /**一键式启动运行模式: 
     * 适合多级菜单程序, 使用此模式后无法手动控制.
     */
    public void run() {
        this.updateMenusHerarchy();  //更新菜单层级关系
        List<ConsoleUI.MenuItem> menus = this.getMainMenus();
        this.welcome();
        this.showMenu();
        int option = 0;
        while(true) {
            option = this.selectMenu(menus.size());
            menus = menus.get(option).click();
        }
    }
    
    
    
    public final static Scanner input = new Scanner(System.in);
    private static String line;
    private String applicationName;
    private List<MenuItem> mainMenus;
    private String version; // 未设置将不显示
    private int length = 36; // 菜单板式长度, 建议偶数
    private int typography; // 文字选项的居中右对齐占位长度," 12．"
    private String welcomeBraid = "*";
    private String menuBraid = "=";
    
    /**
     * 增加子菜单选项(无限).
     * 尾后自动追加"退出"选项.
     * 注意多级菜单层级关系更新问题
     * @param specifiedItem 约定传参格式:"主菜单->子菜单->子菜单..."
     * @param subMenus 请传入一组子菜单集.
     */
    public void addSubMenuItems(String specifiedItem, MenuItem[] subMenus) {
        String[] specifiedItems = specifiedItem.split("->");
        int index = mainMenus.indexOf(new MenuItem(specifiedItems[0]));
        assert index != -1 : "指定菜单项不存在!";
        int subLen = specifiedItems.length-1;
        if(subLen == 0) {  //只有主菜单
            MenuItem item = this.mainMenus.get(index);
            item.addSubMenuItems(null, subMenus);
        }
        else {
            String[] subItems = new String[subLen];
            System.arraycopy(specifiedItems, 1, subItems, 0, subLen);
            this.mainMenus.get(index).addSubMenuItems(subItems, subMenus);
        }
    }

    public void setWelcomeBraid(String welcomeBraid){
        this.welcomeBraid = welcomeBraid;
    }

    /** 默认"=" */
    public void setMenuBraid(String menuBraid) {
        this.menuBraid = menuBraid;
    }
    
    /**取消动态设置菜单项(控制台没必要)*/
    @SuppressWarnings("unused")
    private void setMenu(MenuItem... option) {  }

    /**
     * 如果传入奇数, 自动纠正为不大于其本身的最大偶数.
     * @param length
     */
    public void setUiLength(int length) {
        if(length <= 0) {
            throw new IllegalArgumentException("长度不能<=0!");
        }
        if (length % 2 != 0) {
            length -= 1;
        }
        this.length = length;
    }

    public void setVersion(String number) {
        version = number;
    }
    
    protected List<MenuItem> getMainMenus() {
        return this.mainMenus;
    }

    /**
     * 根据选项长度自动智能确定typography成员值
     * 
     * @param blankCount
     */
    private void intelligentCompose(List<String> items) {
        if(items==null || items.size()==0) {
            typography = 0;
            return;
        }
        int spaces = (length - (3 + 2 * midStrLen(items))) / 2; // "序号．"=3
        typography = spaces + 3;
    }
    
    public void setApplicationName(String name) {
        this.applicationName = ((name==null) ? "" : name);
    }
    
    /**
     * 创建一个无菜单项的控制台程序,
     * 可在后续通过addMainMenu设置添加菜单.
     * @param programName
     */
    public ConsoleUI(String programName) { 
        this.setApplicationName(programName);
        this.addMainMenus(new MenuItem[0]);
    }
    
    /** 默认"0.退出" */
    public void setExitItemName(String newName) {
        mainMenus.get(mainMenus.size()-1).setMenuName(newName);
    }
    
    /**自动追加"退出"选项. 注意多级菜单层级关系更新问题*/
    public void addMainMenus(MenuItem[] option) {
        List<MenuItem> menuList = option==null ?
                new ArrayList<ConsoleUI.MenuItem>() : new ArrayList<MenuItem>(Arrays.asList(option));
        //创建"退出"选项并绑定功能.
        MenuItem exitMenu = new MenuItem("退出");
        exitMenu.bind(()->{
            System.out.println("确定退出程序吗?");
            if(ConsoleUI.boolInput()) {
                System.exit(0);
            }
        });
        menuList.add(exitMenu);
        mainMenus = menuList;
    }
        
    /**
     * 在添加完/改动所有菜单集后,请调用此方法建立父子菜单关系.
     * 否则所有的返回菜单和显示当前菜单功能失效并有可能抛出异常.
     */
    protected void updateMenusHerarchy() {
        //设置主菜单的父层菜单
        for (MenuItem i : mainMenus) {
            i.setCurrentMenuItems(this.mainMenus);
        }
        for (int i = 0; i < mainMenus.size()-1; i++) {  //不含"退出"选项
            MenuItem menuItem = mainMenus.get(i);
            ConsoleUI.updateMenusHerarchy(this.mainMenus, menuItem.getContextMenuItems());
        }
    }
    
    /**
     * 更新此菜单项的上下层/当前层菜单集指向.
     * 在父子层菜单添加完成/发生改动后,调用此方法.
     * 否则"返回上一级.."菜单项将功能失效且抛出异常:
     *     "既未绑定功能,也未设置子菜单集".
     *  @param contextMenus 普通菜单项传入子层菜单, 返回菜单传入父层菜单
     *  @param currentMenus 当前菜单项所在的菜单层
     *  @return true:此菜单项含有子菜单集
     *          false:已经是最后一层
     *///实质上的静态方法--因为不是ConsoleUI对外的工具方法,所以设为private static方法.
    private static void updateMenusHerarchy(List<MenuItem> fatherMenuItems, List<MenuItem> subMenuItems) {  //包私有,避免外部调用
        if(subMenuItems == null)  return;  //结束递归
        
        for (int i = 0; i < subMenuItems.size()-1; i++) {  //size()-1:单独处理返回菜单项
            MenuItem menuItem = subMenuItems.get(i);
            menuItem.setCurrentMenuItems(subMenuItems); //contextMenu已在addMenuItems时设置.
            updateMenusHerarchy(subMenuItems, menuItem.getContextMenuItems()); //递归
        }
        //返回菜单项单独处理
        MenuItem backMenu = subMenuItems.get(subMenuItems.size()-1);
        backMenu.setContextMenuItems(fatherMenuItems);
        //return this.contextMenuItems;
    }


    /**
     * 程序启动首次显示 程序名、版本号.
     */
    public void welcome() {
        try {
            int pauseTime = 500;
            Thread.sleep(pauseTime);
            System.out.println(welcomeBraid.repeat(length));
            Thread.sleep(pauseTime);
            System.out.println(welcomeBraid + " ".repeat(length-2) + welcomeBraid);
            Thread.sleep(pauseTime);
            int spaceCount = (length - 2 * (applicationName.length() + 5)) / 2 - 1;
            System.out.printf("%s欢迎使用%s！%s%n", welcomeBraid + " ".repeat(spaceCount), applicationName,
                    " ".repeat(spaceCount) + welcomeBraid); // 一个中文字符/标点等于2个英文
            if (version != null) {
                System.out.printf(welcomeBraid + "%" + (length - 1) + "s%n", "V" + version + welcomeBraid);
            }
            System.out.println(welcomeBraid + " ".repeat(length-2) + welcomeBraid);
            Thread.sleep(pauseTime);
            System.out.println(welcomeBraid.repeat(length));
            Thread.sleep(pauseTime*4);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * 显示的程序主菜单集.
     * 默认竖排显示.
     * @return 程序主菜单集
     */
    public List<MenuItem> showMenu() {
        return this.showMenu(mainMenus);
    }
    
    /**
     * 显示下一层子菜单
     * @param option 
     * @return 下一层子菜单
     */
    protected List<MenuItem> showMenu(int option) {
        return mainMenus.get(option).showMenu();
    }
    
    private String menuTitle = "功能选项";  
    
    /**替换默认的"功能选项"标题.*/
    public void setMenuTitle(String newTilte) {
        menuTitle = newTilte;
    }
    
    private boolean verticalShowMenu = true;
    
    /**
     * 设置竖排还是横排显示菜单.
     * 默认竖排.
     * @param isVertical
     */
    public void setVerticalShowMenu(boolean isVertical) {
        verticalShowMenu = isVertical;
    }
    
    /**
     * 显示传入的菜单集. 准静态方法.
     * @param menus 传入的菜单集
     * @return 传入的菜单集menus
     */
    protected List<MenuItem> showMenu(List<MenuItem> menus) {
        List<String> menuNames = new ArrayList<String>(menus.size());
        for (MenuItem menuItem : menus) {
            menuNames.add(menuItem.getMenuName());
        }
        
        intelligentCompose(menuNames);  //智能居中
        int quantity = (length - menuTitle.length()*2) / 2;  //"功能选项"=8
        if(menuBraidVisible) System.out.println(menuBraid.repeat(quantity) + listTile + menuBraid.repeat(quantity));
        //显示功能选项, 以.为界居中对齐
        for (int i = 0; i < menus.size() - 1; ++i) {
            if(verticalShowMenu) System.out.printf("%" + typography + "s%s%n", i + 1 + ".", menus.get(i).getMenuName());  //竖排显示
            else System.out.printf("%d.%s ", i+1, menus.get(i));  //横排显示
        }
        if(verticalShowMenu) System.out.printf("%" + typography + "s%s%n", "0.", menus.get(menus.size() - 1)); //"退出"选项
        else System.out.printf("0.%s%n", menus.get(menus.size() - 1)); //"退出"选项
        if(menuBraidVisible) System.out.println(menuBraid.repeat(length));
        
        return menus;
    }
    
    private boolean menuBraidVisible = true;
    
    /**
     * 设置是否显示菜单选项边框. 默认显示.
     * 一般在列表横排显示时隐藏.
     * @param visible
     */
    public void setMenuBraidVisible(boolean visible) {
        menuBraidVisible = visible;
    }
    
    private String listTile = "列表";
    
    /**设置showList()的标题栏. 默认"列表".*/
    public void setListTile(String newListTitle) {
        listTile = newListTitle;
    }
    
    private String listBraid = "=";
    
    /**设置列表边框修饰, 默认"=" */
    public void setListBraid(String newBraid) {
        listBraid = newBraid;
    }

    /**
     * 以竖排或横排列表等距显示居中显示任何内容.(准静态方法)
     * @param items  待显示的列表
     * @param title  列表标题
     * @param listBraid  边框修饰
     * @param braidVisible 是否显示边框. 一般在列表横排显示时隐藏.
     * @param verticalShow 竖排/横排显示列表
     */
    public void showList(List<String> items) {
        intelligentCompose(items);  //智能居中于UI长度
        int quantity = (length - listTile.length()*2) / 2;  //"功能选项"=8
        System.out.println(listBraid.repeat(quantity) + listTile + listBraid.repeat(quantity));
        //行首自定义插入文本
        if(topPos) {
            for (String text : insertText) {
                System.out.println(text);
            }
        }
        //显示功能选项, 以.为界居中对齐
        for (int i = 0; i < items.size(); ++i) {
            System.out.printf("%" + typography + "s%s%n", i + 1 + ".", items.get(i));
        }
        //行尾自定义插入文本
        if(!topPos) {
            for (String text : insertText) {
                System.out.println(text);
            }
        }
        System.out.println(listBraid.repeat(length));
    }
    
    //showList()插入的一至多行文本
    private String[] insertText = {};
    private boolean topPos;
    /**
     * 在showList()中的列表中插入.
     * @param lines  待插入的一至多行文本
     * @param topPos  true:列表行首  false:列表行尾
     */
    public void setInsertText(boolean topPos, String ...lines) {
        if(lines == null) return;
        this.topPos = topPos;
        this.insertText = lines;
    }
    
    /**
     * 主要用于insertLines()插入文本的排版.
     */
    public int getUiLength() {
        return length;
    }

    /**
     * 合法输入:Yes, No--不区分大小写 Y, N--不区分大小写
     */
    public static boolean boolInput() {
        System.out.print("请输入(Y/N):");
        var arlst = new ArrayList<String>(Arrays.<String>asList("YES", "Y", "NO", "N")); // 合法输入
        while (true) {
            line = input.nextLine().trim().toUpperCase();
            if (arlst.contains(line)) {
                return line.equals("YES") || line.equals("Y");
            }
            System.out.println("无效输入! 请输入Yes、No、Y、N其中之一,不区分大小写.");
            System.out.print("请重新输入:");
        }
    }
    
    /**
     * 对主菜单/子菜单选项进行输入.
     * 对"0.退出项"进行了特殊处理,返回正确对应的选项序号.
     * 控制台输入数字,并返回其对应菜单选项号. --实质上的静态方法.
     * @param menuQuantity  主菜单/子菜单选项数量(含退出/返回选项)
     */
    public int selectMenu(int menuQuantity) {
        int maxNumber = menuQuantity - 1;
        int number = ConsoleUI.numberInput(maxNumber);
        return number==0 ? maxNumber : number-1;
    }
    
    /**
     * 对主菜单/子菜单选项进行输入.
     * 对"0.退出项"进行了特殊处理,返回正确对应的选项序号.
     * 控制台输入数字,并返回其对应菜单选项号. --实质上的静态方法.
     * @param menuQuantity  主菜单/子菜单选项数量(含退出/返回选项)
     * @param tip 自定义输入提示语(默认"请输入功能选项号:")
     */
    public int selectMenu(int menuQuantity, String tip) {
        int maxNumber = menuQuantity - 1;
        int number = ConsoleUI.numberInput(0, maxNumber, tip);
        return number==0 ? maxNumber : number-1;
    }
    
    /**
     * 合法输入: 0~end之间(含两者)的数字
     *          序号0一般用于取消/返回
     */
    public static int numberInput(int end) {
        return numberInput(0, end);
    }
    
    /**
     * 合法输入: bgn~end之间(含两者)的数字
     * bgn和end可以相等--单选.
     */
    public static int numberInput(int bgn, int end) {
        return numberInput(bgn, end, "请输入功能选项号:");
    }
    
    /**
     * 合法输入: bgn~end之间(含两者)的数字
     * bgn和end可以相等--单选.
     * @param bgn
     * @param end
     * @param tip 自定义输入提示语(默认"请输入功能选项号:")
     * @return
     */
    public static int numberInput(int bgn, int end, String tip) {
        if(bgn<0  || bgn > end) throw new IllegalArgumentException();
        System.out.print(tip);
        int opt;
        while (true) {
            if (input.hasNextLine()) {
                line = input.nextLine().trim();
                //System.err.println("|" + line + "|");
                try {
                    opt = Integer.valueOf(line, 10);
                    if (bgn <= opt && opt <= end) return opt;
                } catch (NumberFormatException e) { }// 待优化:使用异常判断效率较低
                System.out.println("选项无效!");
                if(bgn != end) System.out.printf("请重新输入(%d~%d):", bgn, end);
                else System.out.printf("请重新输入(%d):", bgn);
            }
        }
    }
    
    /**
     * 阻塞等待键入
     */
    public static void anyKeyInput() {
        System.out.print("按回车键继续...");
        ConsoleUI.input.nextLine();
    }

    /**
     * 暂未实现.
     */
    public static void passwordInput() {
        System.out.println("功能暂未实现!");
        ;
    }

    /**
     * 计算菜单项(不含"0.退出项")字符串长度的中位数
     */
    private int midStrLen(List<String> menus) {
        if (menus.size() <= 1) {
            return menus.get(0).length();
        }
        int[] lens = new int[menus.size() - 1];
        for (int i = 0; i < menus.size() - 1; ++i) { // 排除"0.退出"项
            lens[i] = menus.get(i).length();
        }
        // 使用冒泡排序--从小到大
        for (int i = 0; i < lens.length; ++i) {
            for (int j = 0; j < lens.length - i - 1/*(lens.length + 1) / 2-1*/; ++j) { // 排序一半即可得到中位数
                int tmp = 0;
                if (lens[j] > lens[j + 1]) {
                    tmp = lens[j];
                    lens[j] = lens[j + 1];
                    lens[j + 1] = tmp;
                }
            }
        }
        // if(lens[i] > len)

        return lens[(lens.length - 1) / 2]; // 注意数组序号从0开始
    }
    

    /**
     * 菜单项.
     * 只能具有执行绑定功能或显示父/子层菜单其中一种功能.
     * (所以成员function和contextMenuItems两者之间有且只有一个为null.)
     * 设计难点：
     * 返回菜单项功能的实现.
     * @author HW
     */
    public class MenuItem{
        private String name;
        /**保存当前菜单项所在的菜单集*/
        private List<MenuItem> currentMenuItems = null;
        /**保存普通菜单保存下层菜单集,只有返回菜单保存上层菜单*/
        private List<MenuItem> contextMenuItems = null; 
        /**保存每个菜单绑定的功能*/
        private Function function = null; 

        /**
         * 给菜单项绑定功能.
         * @param function
         */
        public void bind(Function function) {
            if(function == null) throw new MenuFunctionBindFailException("传入功能参数为null");
            if(this.currentMenuItems != null) throw new MenuFunctionBindFailException("此菜单已设置子菜单集,无法绑定功能!");
            this.function = function;
        }
        
        /**
         * 执行菜单项功能或显示上/下层菜单.
         * @return 当前显示的菜单集
         */
        public List<MenuItem> click(){
            if(this.function == null) {
                return this.showOptionMenu();
            }
            else {
                this.execute();
                return this.showMenu();
            }
        }
        
        /**
         * 显示子菜单或执行功能.
         * 应先调用isExecutable进行判断.
         * protected成员函数,指明需要重新覆盖(如添加
         */
        protected void execute(){
            if(this.function == null) throw new IllegalCallerException(this.name + "功能未绑定");
            function.function();
        }
        
        @SuppressWarnings("unused")
        private MenuItem() {  }
        
        public MenuItem(String menuName) {
            super();
            this.name = menuName;
        }
        
        /**
         * @param function 要绑定的功能
         */
        public MenuItem(String menuName, Function function) {
            this.name = menuName;
            this.bind(function);
        }
        
        public MenuItem(String menuName, ConsoleUI.MenuItem ...subMenuItems) {
            this.name = menuName;
            this.addSubMenuItems(null, subMenuItems);
        }

        protected void setCurrentMenuItems(List<MenuItem> menuItems) {
            if(menuItems == null) throw new IllegalArgumentException("传入菜单集为空！");
            //这是currentMenuItems,即使绑定功能也再可以设置.
            this.currentMenuItems = menuItems;
        }

        protected void setContextMenuItems(List<MenuItem> menuItems) {
            if(menuItems == null) throw new IllegalArgumentException("传入菜单集为空！");
            if(this.function != null) throw new IllegalArgumentException(this.name + "已绑定功能,无法设置子菜单集！");
            this.contextMenuItems = menuItems;
        }
        
        protected List<MenuItem> getCurrentMenuItems() {
            return currentMenuItems;
        }

        protected List<MenuItem> getContextMenuItems() {
            return contextMenuItems;
        }

        /**
         * 显示当前菜单项所在的菜单集
         * @return 显示的菜单集
         */
        protected List<MenuItem> showMenu() {
            //if(this.currentMenuItems == null) throw new IllegalCallerException(this.name);
            if(this.currentMenuItems == null) return null;  //改动: 静默返回, 不抛异常.
            return ConsoleUI.this.showMenu(this.currentMenuItems);
        }
        
        /**
         * 显示上/下层菜单集
         * @return 显示的菜单集
         */
        protected List<MenuItem> showOptionMenu() {
            if(this.contextMenuItems == null) throw new IllegalCallerException(this.name);
               return ConsoleUI.this.showMenu(this.contextMenuItems);  
        }
        
        public String getMenuName() {
            return name;
        }

        public void setMenuName(String name) {
            if(name == null)  throw new IllegalArgumentException("菜单名为null");
            this.name = name;
        }

        /**
         * 在指定指定菜单项后面追加子菜单集,
         * 尾后自动追加"返回上一级菜单.."选项.
         * 注意:
         * 已移除了建立父子层菜单关系的功能,
         * 请在所有菜单集添加完成后通过调用Console.updateMenuHerarchy()建立关系.
         * 注意多级菜单层级关系更新问题.
         * @param specifiedItems 不含主菜单,当传入null值时,在当前项尾后添加子菜单集.
         * @param subItems 请传入一组子菜单集
         */
        public void addSubMenuItems(String[] specifiedItems, MenuItem... subItems) {
            if(specifiedItems == null) { //已递归达到子菜单末尾
                assert this.contextMenuItems==null : this.name + "已存在子菜单项,不能重复添加!";
                if(subItems == null) throw new IllegalArgumentException("传入子菜单集不能空!");
                if(this.function != null) throw new IllegalCallerException("菜单项功能已绑定,无法再添加子菜单集!");
                if(subItems.length == 0) return;
                
                List<MenuItem> subItemsList = new ArrayList<MenuItem>(Arrays.asList(subItems));
                MenuItem backMenu = new MenuItem("返回上一级..");
                subItemsList.add(backMenu);
                this.contextMenuItems = subItemsList;
                backMenu.setCurrentMenuItems(subItemsList);
                return;
            }
            
            //递归调用
            int index = contextMenuItems.indexOf(new MenuItem(specifiedItems[0])); 
            assert index!=-1 : "指定菜单项不存在!";
            int sublen = specifiedItems.length -1;
            if(sublen <= 0) {
                this.contextMenuItems.get(index).addSubMenuItems(null, subItems);
            }
            else {
                String[] subSpecifiedItems = new String[sublen];
                System.arraycopy(specifiedItems, 1, subSpecifiedItems, 0, sublen);
                this.contextMenuItems.get(index).addSubMenuItems(subSpecifiedItems, subItems);
            }
        }

        @Override
        public String toString() {
            return name;
        }

    }

}