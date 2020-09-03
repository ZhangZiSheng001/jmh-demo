package cn.zzs.jmh;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

/**
 * 测试动态代理类执行速度
 * @author zzs
 * @date 2020年9月3日 下午3:40:51
 */
public class DynamicProxyInvokeTest {
    
    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void invokeJdkDynamicProxy(JdkDynamicProxyState state) {
        state.countService.count();
    }
    
    
    @State(Scope.Benchmark)
    public static class JdkDynamicProxyState {
        
        CountService countService;
       
        @Setup(Level.Trial)
        public void init() {
            this.countService = createJdkDynamicProxy();
        }
        
        private CountService createJdkDynamicProxy() {
            CountService jdkProxy = (CountService) Proxy.newProxyInstance(
                    ClassLoader.getSystemClassLoader(), 
                    CountServiceImpl.class.getInterfaces(), 
                    new JdkHandler(new CountServiceImpl())
                    );
            return jdkProxy;
        }
        
        private static class JdkHandler implements InvocationHandler {

            final Object delegate;

            JdkHandler(Object delegate) {
                this.delegate = delegate;
            }

            public Object invoke(Object object, Method method, Object[] objects)
                    throws Throwable {
                return method.invoke(delegate, objects);
            }
        }
    }
    

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void invokeCglibDynamicProxy(CglibDynamicProxyState state) {
        state.countService.count();
    }
    
    @State(Scope.Benchmark)
    public static class CglibDynamicProxyState {
        
        CountService countService;
       
        @Setup(Level.Trial)
        public void init() {
            this.countService = createCglibDynamicProxy();
        }
        
        private CountService createCglibDynamicProxy() {  
            Enhancer enhancer = new Enhancer();  
            enhancer.setCallback(new CglibInterceptor());  
            enhancer.setSuperclass(CountServiceImpl.class);  
            CountService cglibProxy = (CountService) enhancer.create();  
            return cglibProxy;  
        }  
      
        
        private static class CglibInterceptor implements MethodInterceptor {  
            
            public Object intercept(Object object, Method method, Object[] objects,  
                    MethodProxy methodProxy) throws Throwable {  
                return methodProxy.invokeSuper(object, objects);  
            }  
        } 
    }


    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void invokeJavassistBytecodeDynamicProxy(JavassistBytecodeDynamicProxyState state) {
        state.countService.count();
    }
    
    @State(Scope.Benchmark)
    public static class JavassistBytecodeDynamicProxyState {
        
        CountService countService;
       
        @Setup(Level.Trial)
        public void init() throws Exception {
            this.countService = createJavassistBytecodeDynamicProxy();
        }
        
        private CountService createJavassistBytecodeDynamicProxy() throws Exception {
            ClassPool mPool = new ClassPool(true);
            CtClass mCtc = mPool.makeClass(CountService.class.getName() + "JavaassistProxy");
            mCtc.addInterface(mPool.get(CountService.class.getName()));
            mCtc.addConstructor(CtNewConstructor.defaultConstructor(mCtc));
            mCtc.addField(CtField.make("public " + CountService.class.getName() + " delegate;", mCtc));
            mCtc.addMethod(CtNewMethod.make("public int count() { return delegate.count(); }", mCtc));
            Class<?> pc = mCtc.toClass();
            CountService bytecodeProxy = (CountService) pc.newInstance();
            Field filed = bytecodeProxy.getClass().getField("delegate");
            filed.set(bytecodeProxy, new CountServiceImpl());
            return bytecodeProxy;
        }
    }
 
    
    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void invokeJavassistDynamicProxy(JavassistDynamicProxyState state) {
        state.countService.count();
    }
    
    @State(Scope.Benchmark)
    public static class JavassistDynamicProxyState {
        
        CountService countService;
       
        @Setup(Level.Trial)
        public void init() throws Exception {
            this.countService = createJavassistDynamicProxy();
        }
        
        private CountService createJavassistDynamicProxy() throws Exception {
            ProxyFactory proxyFactory = new ProxyFactory();
            proxyFactory.setInterfaces(CountServiceImpl.class.getInterfaces());
            Class<?> proxyClass = proxyFactory.createClass();
            CountService javassistProxy = (CountService) proxyClass.newInstance();
            ((ProxyObject) javassistProxy).setHandler(new JavaAssitInterceptor(new CountServiceImpl()));
            return javassistProxy;
        }
        
        private static class JavaAssitInterceptor implements MethodHandler {

            final Object delegate;

            JavaAssitInterceptor(Object delegate) {
                this.delegate = delegate;
            }

            public Object invoke(Object self, Method m, Method proceed,
                                 Object[] args) throws Throwable {
                return m.invoke(delegate, args);
            }
        }
    }

    
    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void invokeAsmBytecodeDynamicProxy(AsmBytecodeDynamicProxyState state) {
        state.countService.count();
    }

    @State(Scope.Benchmark)
    public static class AsmBytecodeDynamicProxyState {
        
        CountService countService;
       
        @Setup(Level.Trial)
        public void init() throws Exception {
            this.countService = createAsmBytecodeDynamicProxy();
        }
        
        public CountService createAsmBytecodeDynamicProxy() throws Exception {
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            String className = CountService.class.getName() + "AsmProxy";
            String classPath = className.replace('.', '/');
            String interfacePath = CountService.class.getName().replace('.', '/');
            classWriter.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC, classPath, null, "java/lang/Object", new String[]{interfacePath});
            
            MethodVisitor initVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
            initVisitor.visitCode();
            initVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            initVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            initVisitor.visitInsn(Opcodes.RETURN);
            initVisitor.visitMaxs(0, 0);
            initVisitor.visitEnd();
            
            FieldVisitor fieldVisitor = classWriter.visitField(Opcodes.ACC_PUBLIC, "delegate", "L" + interfacePath + ";", null, null);
            fieldVisitor.visitEnd();
            
            MethodVisitor methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "count", "()I", null, null);
            methodVisitor.visitCode();
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitFieldInsn(Opcodes.GETFIELD, classPath, "delegate", "L" + interfacePath + ";");
            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, interfacePath, "count", "()I", true);
            methodVisitor.visitInsn(Opcodes.IRETURN);
            methodVisitor.visitMaxs(0, 0);
            methodVisitor.visitEnd();
            
            classWriter.visitEnd();
            byte[] code = classWriter.toByteArray();
            CountService bytecodeProxy = (CountService) new ByteArrayClassLoader().getClass(className, code).newInstance();
            Field filed = bytecodeProxy.getClass().getField("delegate");
            filed.set(bytecodeProxy, new CountServiceImpl());
            return bytecodeProxy;
        }
        
        private static class ByteArrayClassLoader extends ClassLoader {
            
            public ByteArrayClassLoader() {
                super(ByteArrayClassLoader.class.getClassLoader());
            }
            
            public synchronized Class<?> getClass(String name, byte[] code) {
                if (name == null) {
                    throw new IllegalArgumentException("");
                }
                return defineClass(name, code, 0, code.length);
            }
            
        }
    }
    
    
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(DynamicProxyInvokeTest.class.getSimpleName())
                .warmupTime(TimeValue.seconds(1L))
                .measurementTime(TimeValue.seconds(1L))
                .forks(1)
                .build();

        new Runner(opt).run();
    }
}
