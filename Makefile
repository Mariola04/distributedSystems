# Distributed Systems Assignment Makefile
# Compiles and runs Token Ring, P2P, and Total Order Multicast implementations

# Java compiler and runtime
JAVAC = javac
JAVA = java
PROTOC = protoc
JAVAC_FLAGS = -Xlint:unchecked -Xlint:deprecation

# Directory structure
SRCDIR = src
BUILDDIR = build
LIBDIR = lib
PROTODIR = src/ds/assignment/tring/proto

# gRPC dependencies (download these JARs to lib/)
GRPC_VERSION = 1.58.0
PROTOBUF_VERSION = 3.24.0

# Required JAR files
GRPC_JARS = grpc-netty-shaded-$(GRPC_VERSION).jar \
           grpc-protobuf-$(GRPC_VERSION).jar \
           grpc-stub-$(GRPC_VERSION).jar \
           protobuf-java-$(PROTOBUF_VERSION).jar \
           grpc-core-$(GRPC_VERSION).jar \
           grpc-api-$(GRPC_VERSION).jar \
           guava-32.1.2-jre.jar \
           annotations-4.1.1.4.jar

# Classpath setup
CLASSPATH = $(BUILDDIR):$(LIBDIR)/*:.

# Find all Java source files
SOURCES = $(shell find $(SRCDIR) -name "*.java" 2>/dev/null)
CLASSES = $(SOURCES:$(SRCDIR)/%.java=$(BUILDDIR)/%.class)

# Package paths
TRING_PKG = ds.assignment.tring
P2P_PKG = ds.assignment.p2p
TOM_PKG = ds.assignment.tom

# Proto files
PROTO_FILES = $(shell find $(PROTODIR) -name "*.proto" 2>/dev/null)
PROTO_JAVA_FILES = $(PROTO_FILES:$(PROTODIR)/%.proto=$(SRCDIR)/%.java)

# Default target
all: compile

# Create necessary directories
$(BUILDDIR):
	mkdir -p $(BUILDDIR)

$(LIBDIR):
	mkdir -p $(LIBDIR)

# Setup gRPC dependencies
setup-grpc: $(LIBDIR)
	@echo "Setting up gRPC dependencies..."
	@echo "Please download the following JAR files to $(LIBDIR)/:"
	@echo "1. gRPC Core: https://repo1.maven.org/maven2/io/grpc/grpc-netty-shaded/$(GRPC_VERSION)/"
	@echo "2. gRPC Protobuf: https://repo1.maven.org/maven2/io/grpc/grpc-protobuf/$(GRPC_VERSION)/"
	@echo "3. gRPC Stub: https://repo1.maven.org/maven2/io/grpc/grpc-stub/$(GRPC_VERSION)/"
	@echo "4. Protobuf Java: https://repo1.maven.org/maven2/com/google/protobuf/protobuf-java/$(PROTOBUF_VERSION)/"
	@echo "5. gRPC Core: https://repo1.maven.org/maven2/io/grpc/grpc-core/$(GRPC_VERSION)/"
	@echo "6. gRPC API: https://repo1.maven.org/maven2/io/grpc/grpc-api/$(GRPC_VERSION)/"
	@echo "Or run: make download-grpc"

# Download gRPC dependencies automatically
download-grpc: $(LIBDIR)
	@echo "Downloading gRPC dependencies..."
	wget -P $(LIBDIR) https://repo1.maven.org/maven2/io/grpc/grpc-netty-shaded/$(GRPC_VERSION)/grpc-netty-shaded-$(GRPC_VERSION).jar
	wget -P $(LIBDIR) https://repo1.maven.org/maven2/io/grpc/grpc-protobuf/$(GRPC_VERSION)/grpc-protobuf-$(GRPC_VERSION).jar
	wget -P $(LIBDIR) https://repo1.maven.org/maven2/io/grpc/grpc-stub/$(GRPC_VERSION)/grpc-stub-$(GRPC_VERSION).jar
	wget -P $(LIBDIR) https://repo1.maven.org/maven2/com/google/protobuf/protobuf-java/$(PROTOBUF_VERSION)/protobuf-java-$(PROTOBUF_VERSION).jar
	wget -P $(LIBDIR) https://repo1.maven.org/maven2/io/grpc/grpc-core/$(GRPC_VERSION)/grpc-core-$(GRPC_VERSION).jar
	wget -P $(LIBDIR) https://repo1.maven.org/maven2/io/grpc/grpc-api/$(GRPC_VERSION)/grpc-api-$(GRPC_VERSION).jar
	wget -P $(LIBDIR) https://repo1.maven.org/maven2/com/google/guava/guava/32.1.2-jre/guava-32.1.2-jre.jar
	wget -P $(LIBDIR) https://repo1.maven.org/maven2/org/jetbrains/annotations/4.1.1.4/annotations-4.1.1.4.jar
	@echo "gRPC dependencies downloaded successfully!"

# Compile protocol buffers
compile-proto: $(PROTO_FILES)
	@if [ -n "$(PROTO_FILES)" ]; then \
		echo "Compiling protocol buffers..."; \
		for proto in $(PROTO_FILES); do \
			$(PROTOC) --java_out=$(SRCDIR) --grpc-java_out=$(SRCDIR) --plugin=protoc-gen-grpc-java=/tmp/protoc-gen-grpc-java $$proto; \
		done; \
		echo "Proto compilation complete."; \
	else \
		echo "No .proto files found."; \
	fi

# Compile all Java files
compile: $(BUILDDIR)
	@echo "Compiling Java files..."
	@mkdir -p $(BUILDDIR)
	@find $(SRCDIR) -name "*.java" | xargs $(JAVAC) $(JAVAC_FLAGS) -cp $(CLASSPATH) -d $(BUILDDIR)
	@echo "Compilation complete."

# Token Ring targets
run-tring-server:
	$(JAVA) -cp $(CLASSPATH) $(TRING_PKG).CalculatorServer

run-tring-p1:
	$(JAVA) -cp $(CLASSPATH) $(TRING_PKG).TokenRingPeer p1 8001 localhost:8002 localhost:9000

run-tring-p2:
	$(JAVA) -cp $(CLASSPATH) $(TRING_PKG).TokenRingPeer p2 8002 localhost:8003 localhost:9000

run-tring-p3:
	$(JAVA) -cp $(CLASSPATH) $(TRING_PKG).TokenRingPeer p3 8003 localhost:8004 localhost:9000

run-tring-p4:
	$(JAVA) -cp $(CLASSPATH) $(TRING_PKG).TokenRingPeer p4 8004 localhost:8005 localhost:9000

run-tring-p5:
	$(JAVA) -cp $(CLASSPATH) $(TRING_PKG).TokenRingPeer p5 8005 localhost:8001 localhost:9000

# Launch all token ring components in separate terminals
run-tring-demo: compile
	@echo "Launching Token Ring demonstration in separate terminals..."
	@echo "Opening calculator server..."
	xterm -T "Calculator Server" -e "make run-tring-server; bash" &
	@sleep 1
	@echo "Opening peer p1..."
	xterm -T "Peer p1" -e "make run-tring-p1; bash" &
	@sleep 0.5
	@echo "Opening peer p2..."
	xterm -T "Peer p2" -e "make run-tring-p2; bash" &
	@sleep 0.5
	@echo "Opening peer p3..."
	xterm -T "Peer p3" -e "make run-tring-p3; bash" &
	@sleep 0.5
	@echo "Opening peer p4..."
	xterm -T "Peer p4" -e "make run-tring-p4; bash" &
	@sleep 0.5
	@echo "Opening peer p5..."
	xterm -T "Peer p5" -e "make run-tring-p5; bash" &
	@echo "All terminals launched! Close them individually when done."

# P2P targets  
run-p2p-p1:
	$(JAVA) -cp $(CLASSPATH) $(P2P_PKG).P2PPeer p1 8001

run-p2p-p2:
	$(JAVA) -cp $(CLASSPATH) $(P2P_PKG).P2PPeer p2 8002

run-p2p-p3:
	$(JAVA) -cp $(CLASSPATH) $(P2P_PKG).P2PPeer p3 8003

run-p2p-p4:
	$(JAVA) -cp $(CLASSPATH) $(P2P_PKG).P2PPeer p4 8004

run-p2p-p5:
	$(JAVA) -cp $(CLASSPATH) $(P2P_PKG).P2PPeer p5 8005

run-p2p-p6:
	$(JAVA) -cp $(CLASSPATH) $(P2P_PKG).P2PPeer p6 8006

# Launch P2P demo with convergence test (all peers start with 0 except p1 with 1)
run-p2p-demo: compile
	@echo "Launching P2P Anti-Entropy demonstration in separate terminals..."
	@echo "Opening peer p1 with initial value 1.0..."
	xterm -T "P2P Peer p1 (value=1.0)" -e "make run-p2p-p1-convergence; bash" &
	@sleep 0.5
	@echo "Opening peer p2 with initial value 0.0..."
	xterm -T "P2P Peer p2 (value=0.0)" -e "make run-p2p-p2-convergence; bash" &
	@sleep 0.5
	@echo "Opening peer p3 with initial value 0.0..."
	xterm -T "P2P Peer p3 (value=0.0)" -e "make run-p2p-p3-convergence; bash" &
	@sleep 0.5
	@echo "Opening peer p4 with initial value 0.0..."
	xterm -T "P2P Peer p4 (value=0.0)" -e "make run-p2p-p4-convergence; bash" &
	@sleep 0.5
	@echo "Opening peer p5 with initial value 0.0..."
	xterm -T "P2P Peer p5 (value=0.0)" -e "make run-p2p-p5-convergence; bash" &
	@sleep 0.5
	@echo "Opening peer p6 with initial value 0.0..."
	xterm -T "P2P Peer p6 (value=0.0)" -e "make run-p2p-p6-convergence; bash" &
	@echo "All P2P peers launched! Use 'register <peer-id> <host:port>' to build network topology."

# Convergence test targets following PDF topology (1 peer with value 1.0, others with 0.0)
run-p2p-p1-convergence:
	$(JAVA) -cp $(CLASSPATH) $(P2P_PKG).P2PPeer p1 8001 0.0

run-p2p-p2-convergence:
	$(JAVA) -cp $(CLASSPATH) $(P2P_PKG).P2PPeer p2 8002 0.0 p1:localhost:8001 p3:localhost:8003 p4:localhost:8004

run-p2p-p3-convergence:
	$(JAVA) -cp $(CLASSPATH) $(P2P_PKG).P2PPeer p3 8003 1.0

run-p2p-p4-convergence:
	$(JAVA) -cp $(CLASSPATH) $(P2P_PKG).P2PPeer p4 8004 0.0 p5:localhost:8005 p6:localhost:8006

run-p2p-p5-convergence:
	$(JAVA) -cp $(CLASSPATH) $(P2P_PKG).P2PPeer p5 8005 0.0

run-p2p-p6-convergence:
	$(JAVA) -cp $(CLASSPATH) $(P2P_PKG).P2PPeer p6 8006 0.0

# Total Order Multicast (Chat Application) targets
run-tom-p1:
	$(JAVA) -cp $(CLASSPATH) $(TOM_PKG).ChatPeer p1 8081 p2:localhost:8082 p3:localhost:8083 p4:localhost:8084 p5:localhost:8085 p6:localhost:8086

run-tom-p2:
	$(JAVA) -cp $(CLASSPATH) $(TOM_PKG).ChatPeer p2 8082 p1:localhost:8081 p3:localhost:8083 p4:localhost:8084 p5:localhost:8085 p6:localhost:8086

run-tom-p3:
	$(JAVA) -cp $(CLASSPATH) $(TOM_PKG).ChatPeer p3 8083 p1:localhost:8081 p2:localhost:8082 p4:localhost:8084 p5:localhost:8085 p6:localhost:8086

run-tom-p4:
	$(JAVA) -cp $(CLASSPATH) $(TOM_PKG).ChatPeer p4 8084 p1:localhost:8081 p2:localhost:8082 p3:localhost:8083 p5:localhost:8085 p6:localhost:8086

run-tom-p5:
	$(JAVA) -cp $(CLASSPATH) $(TOM_PKG).ChatPeer p5 8085 p1:localhost:8081 p2:localhost:8082 p3:localhost:8083 p4:localhost:8084 p6:localhost:8086

run-tom-p6:
	$(JAVA) -cp $(CLASSPATH) $(TOM_PKG).ChatPeer p6 8086 p1:localhost:8081 p2:localhost:8082 p3:localhost:8083 p4:localhost:8084 p5:localhost:8085

# Run chat application test
test-tom-chat:
	$(JAVA) -cp $(CLASSPATH) $(TOM_PKG).TestChatApplication

# Test malicious peer detection [EXTRA MARKS]
test-tom-security:
	./test_malicious_peer.sh

# Visual security demo in terminals [EXTRA MARKS]
run-tom-security-demo:
	./run_security_demo.sh

# Launch all TOM chat peers in separate terminals
run-tom-demo: compile
	@echo "Launching Total Order Multicast Chat demonstration in separate terminals..."
	@echo "Opening chat peer p1..."
	xterm -T "Chat Peer p1" -e "make run-tom-p1; bash" &
	@sleep 0.5
	@echo "Opening chat peer p2..."
	xterm -T "Chat Peer p2" -e "make run-tom-p2; bash" &
	@sleep 0.5
	@echo "Opening chat peer p3..."
	xterm -T "Chat Peer p3" -e "make run-tom-p3; bash" &
	@sleep 0.5
	@echo "Opening chat peer p4..."
	xterm -T "Chat Peer p4" -e "make run-tom-p4; bash" &
	@sleep 0.5
	@echo "Opening chat peer p5..."
	xterm -T "Chat Peer p5" -e "make run-tom-p5; bash" &
	@sleep 0.5
	@echo "Opening chat peer p6..."
	xterm -T "Chat Peer p6" -e "make run-tom-p6; bash" &
	@echo "All chat peers launched! Watch them exchange words with total ordering."

# Utility targets
clean:
	rm -rf $(BUILDDIR)
	@echo "Build directory cleaned."

clean-all: clean
	rm -rf $(LIBDIR)
	@echo "All generated files cleaned."

# Help target
help:
	@echo "Available targets:"
	@echo "  compile          - Compile all Java files"
	@echo "  clean           - Remove compiled classes"
	@echo "  clean-all       - Remove all generated files"
	@echo ""
	@echo "Token Ring (run server first):"
	@echo "  run-tring-demo   - Launch ALL components in separate terminals"
	@echo "  run-tring-server - Start calculator server"
	@echo "  run-tring-p1     - Start peer p1"
	@echo "  run-tring-p2     - Start peer p2"
	@echo "  run-tring-p3     - Start peer p3"
	@echo "  run-tring-p4     - Start peer p4"
	@echo "  run-tring-p5     - Start peer p5"
	@echo ""
	@echo "P2P Network:"
	@echo "  run-p2p-demo     - Launch ALL peers in separate terminals (convergence test)"
	@echo "  run-p2p-p1       - Start P2P peer p1"
	@echo "  run-p2p-p2       - Start P2P peer p2"
	@echo "  run-p2p-p3       - Start P2P peer p3"
	@echo "  run-p2p-p4       - Start P2P peer p4"
	@echo "  run-p2p-p5       - Start P2P peer p5"
	@echo "  run-p2p-p6       - Start P2P peer p6"
	@echo ""
	@echo "Total Order Multicast (Chat Application):"
	@echo "  run-tom-demo     - Launch ALL chat peers in separate terminals"
	@echo "  test-tom-chat    - Run chat application test (automated)"
	@echo "  test-tom-security- Test malicious peer detection [EXTRA MARKS]"
	@echo "  run-tom-security-demo - Visual security demo with attack terminals [EXTRA MARKS]"
	@echo "  run-tom-p1       - Start chat peer p1"
	@echo "  run-tom-p2       - Start chat peer p2"
	@echo "  run-tom-p3       - Start chat peer p3"
	@echo "  run-tom-p4       - Start chat peer p4"
	@echo "  run-tom-p5       - Start chat peer p5"
	@echo "  run-tom-p6       - Start chat peer p6"
	@echo ""
	@echo "TESTING:"
	@echo "  test-calculator  - Test calculator server operations"
	@echo "  test-poisson     - Test Poisson request generation"
	@echo "  test-basic       - Test basic token ring (30s)"
	@echo "  test-failure     - Test failure recovery [EXTRA MARKS]"
	@echo "  test-all         - Run all token ring tests"

# Testing targets
test-basic:
	@echo "Running basic token ring test..."
	./test/test-basic-token-ring.sh

test-failure:
	@echo "Running failure recovery test [EXTRA MARKS]..."
	./test/test-failure-recovery.sh

test-calculator:
	@echo "Running calculator server test..."
	./test/test-calculator-server.sh

test-poisson:
	@echo "Running Poisson generation test..."
	./test/test-poisson-generation.sh

test-all: test-calculator test-poisson test-basic test-failure
	@echo "All Token Ring tests completed!"

# Check if Java files exist
check-sources:
	@if [ -z "$(SOURCES)" ]; then \
		echo "No Java source files found in $(SRCDIR)"; \
		echo "Make sure you have created your Java classes in the correct package structure."; \
	else \
		echo "Found $(words $(SOURCES)) Java source files"; \
	fi

# Force rebuild
rebuild: clean compile

.PHONY: all compile clean clean-all help check-sources rebuild setup-grpc download-grpc compile-proto \
        run-tring-server run-tring-p1 run-tring-p2 run-tring-p3 run-tring-p4 run-tring-p5 run-tring-demo \
        run-p2p-p1 run-p2p-p2 run-p2p-p3 run-p2p-p4 run-p2p-p5 run-p2p-p6 run-p2p-demo \
        run-p2p-p1-convergence run-p2p-p2-convergence run-p2p-p3-convergence run-p2p-p4-convergence run-p2p-p5-convergence run-p2p-p6-convergence \
        run-tom-p1 run-tom-p2 run-tom-p3 run-tom-p4 run-tom-p5 run-tom-p6 run-tom-demo test-tom-chat test-tom-security run-tom-security-demo \
        test-basic test-failure test-calculator test-poisson test-all