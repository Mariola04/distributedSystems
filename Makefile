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

# Total Order Multicast targets
run-tom-p1:
	$(JAVA) -cp $(CLASSPATH) $(TOM_PKG).TOMPeer p1 8001

run-tom-p2:
	$(JAVA) -cp $(CLASSPATH) $(TOM_PKG).TOMPeer p2 8002

run-tom-p3:
	$(JAVA) -cp $(CLASSPATH) $(TOM_PKG).TOMPeer p3 8003

run-tom-p4:
	$(JAVA) -cp $(CLASSPATH) $(TOM_PKG).TOMPeer p4 8004

run-tom-p5:
	$(JAVA) -cp $(CLASSPATH) $(TOM_PKG).TOMPeer p5 8005

run-tom-p6:
	$(JAVA) -cp $(CLASSPATH) $(TOM_PKG).TOMPeer p6 8006

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
	@echo "  run-tring-server - Start calculator server"
	@echo "  run-tring-p1     - Start peer p1"
	@echo "  run-tring-p2     - Start peer p2"
	@echo "  run-tring-p3     - Start peer p3"
	@echo "  run-tring-p4     - Start peer p4"
	@echo "  run-tring-p5     - Start peer p5"
	@echo ""
	@echo "P2P Network:"
	@echo "  run-p2p-p1       - Start P2P peer p1"
	@echo "  run-p2p-p2       - Start P2P peer p2"
	@echo "  run-p2p-p3       - Start P2P peer p3"
	@echo "  run-p2p-p4       - Start P2P peer p4"
	@echo "  run-p2p-p5       - Start P2P peer p5"
	@echo "  run-p2p-p6       - Start P2P peer p6"
	@echo ""
	@echo "Total Order Multicast:"
	@echo "  run-tom-p1       - Start TOM peer p1"
	@echo "  run-tom-p2       - Start TOM peer p2"
	@echo "  run-tom-p3       - Start TOM peer p3"
	@echo "  run-tom-p4       - Start TOM peer p4"
	@echo "  run-tom-p5       - Start TOM peer p5"
	@echo "  run-tom-p6       - Start TOM peer p6"
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
        run-tring-server run-tring-p1 run-tring-p2 run-tring-p3 run-tring-p4 run-tring-p5 \
        run-p2p-p1 run-p2p-p2 run-p2p-p3 run-p2p-p4 run-p2p-p5 run-p2p-p6 \
        run-tom-p1 run-tom-p2 run-tom-p3 run-tom-p4 run-tom-p5 run-tom-p6 \
        test-basic test-failure test-calculator test-poisson test-all