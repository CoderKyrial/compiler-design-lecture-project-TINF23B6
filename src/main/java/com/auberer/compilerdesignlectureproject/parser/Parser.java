package com.auberer.compilerdesignlectureproject.parser;

import com.auberer.compilerdesignlectureproject.ast.*;
import com.auberer.compilerdesignlectureproject.lexer.ILexer;
import com.auberer.compilerdesignlectureproject.lexer.Token;
import com.auberer.compilerdesignlectureproject.lexer.TokenType;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.Stack;

/**
 * Parser class for building the abstract syntax tree (AST).
 * Input: Token stream
 * Output: AST
 */
@Slf4j
public class Parser implements IParser {

  // Lexer interface that can be used to accept the given input
  ILexer lexer;
  // Stack to keep track of the parent nodes
  Stack<ASTNode> parentStack = new Stack<>();

  public Parser(ILexer lexer) {
    this.lexer = lexer;
  }

  /**
   * Entry point to the parser. This method should parse the input file and return the root node of the AST.
   *
   * @return AST root node
   */
  @Override
  public ASTEntryNode parse() {
    ASTEntryNode node = new ASTEntryNode();
    enterNode(node);

    Set<TokenType> selectionSet = ASTFunctionDefNode.getSelectionSet();
    if (selectionSet.contains(lexer.getToken().getType())) {
      do {
        parseFctDef();
      } while (!lexer.isEOF());
    } else {
      throw new RuntimeException("Need at least one function definition");
    }

    exitNode(node);
    assert parentStack.empty();
    return node;
  }

  private void parseFctDef() {
    ASTFunctionDefNode node = new ASTFunctionDefNode();
    enterNode(node);

    parseType();
    Token token = lexer.getToken();
    if (token.getType() == TokenType.TOK_IDENTIFIER) {
      node.setIdentifier(token.getText());
    } else {
      throw new RuntimeException("Unexpected token type: " + token.getType());
    }
    lexer.expect(TokenType.TOK_IDENTIFIER);
    lexer.expect(TokenType.TOK_COLON);
    lexer.expect(TokenType.TOK_ASSIGN);
    lexer.expect(TokenType.TOK_LPAREN);
    Set<TokenType> selectionSet = ASTParamLstNode.getSelectionSet();
    if (selectionSet.contains(lexer.getToken().getType())) {
      parseParamLst();
    }
    lexer.expect(TokenType.TOK_RPAREN);
    lexer.expect(TokenType.TOK_LBRACE);
    parseStmtLst();
    lexer.expect(TokenType.TOK_RBRACE);

    exitNode(node);
  }

  private void parseFctCall() {
    ASTFunctionCallNode node = new ASTFunctionCallNode();
    enterNode(node);

    lexer.expect(TokenType.TOK_CALL);
    Token token = lexer.getToken();
    if (token.getType() == TokenType.TOK_IDENTIFIER) {
      node.setIdentifier(token.getText());
    } else {
      throw new RuntimeException("Unexpected token type: " + token.getType());
    }
    lexer.expect(TokenType.TOK_IDENTIFIER);
    lexer.expect(TokenType.TOK_LPAREN);
    Set<TokenType> selectionSet = ASTArgLstNode.getSelectionSet();
    if (selectionSet.contains(lexer.getToken().getType())) {
      parseArgLst();
    }
    lexer.expect(TokenType.TOK_RPAREN);
    exitNode(node);
  }

  private void parseParamLst() {
    ASTParamLstNode node = new ASTParamLstNode();
    enterNode(node);

    parseParam();
    while (lexer.getToken().getType().equals(TokenType.TOK_SEMICOLON)) {
      lexer.expect(TokenType.TOK_SEMICOLON);
      parseParam();
    }

    exitNode(node);
  }

  private void parseParam() {
    ASTParamNode node = new ASTParamNode();
    enterNode(node);

    parseType();
    node.setParamName(lexer.getToken().getText());
    lexer.expect(TokenType.TOK_IDENTIFIER);
    if (lexer.getToken().getType() == TokenType.TOK_ASSIGN) {
      lexer.expect(TokenType.TOK_ASSIGN);
      parseAtomicExpr();
    }

    exitNode(node);
  }

  private void parseArgLst() {
    ASTArgLstNode node = new ASTArgLstNode();
    enterNode(node);

    parseAtomicExpr();
    while (lexer.getToken().getType().equals(TokenType.TOK_SEMICOLON)) {
      lexer.expect(TokenType.TOK_SEMICOLON);
      parseAtomicExpr();
    }

    exitNode(node);
  }

  private void parseReturnStmt() {
    ASTReturnStmtNode node = new ASTReturnStmtNode();
    enterNode(node);

    lexer.expect(TokenType.TOK_RET);
    parseTernaryExpr();
    lexer.expect(TokenType.TOK_SEMICOLON);

    exitNode(node);
  }

  private void parseStmtLst() {
    ASTStmtLstNode node = new ASTStmtLstNode();
    enterNode(node);

    Set<TokenType> selectionSet = ASTStmtNode.getSelectionSet();
    while (selectionSet.contains(lexer.getToken().getType())) {
      parseStmt();
    }

    exitNode(node);
  }

  private void parseStmt() {
    ASTStmtNode node = new ASTStmtNode();
    enterNode(node);

    TokenType tokenType = lexer.getToken().getType();
    if (ASTVarDeclNode.getSelectionSet().contains(tokenType)) {
      parseVarDeclStmt();
    } else if (ASTAssignStmtNode.getSelectionSet().contains(tokenType)) {
      parseAssignStmt();
    } else if (ASTReturnStmtNode.getSelectionSet().contains(tokenType)) {
      parseReturnStmt();
    } else if (ASTIfStmtNode.getSelectionSet().contains(tokenType)) {
      parseIfStmt();
    } else if (ASTWhileLoopNode.getSelectionSet().contains(tokenType)) {
      parseWhileLoopStmt();
    } else if (ASTDoWhileLoopNode.getSelectionSet().contains(tokenType)) {
      parseDoWhileLoop();
    } else if (ASTForLoopNode.getSelectionSet().contains(lexer.getToken().getType())) {
      parseForLoop();
    } else if (ASTSwitchCaseStmtNode.getSelectionSet().contains(tokenType)) {
      parseSwitchCaseStmt();
    } else if (ASTAnonymousBlockStmtNode.getSelectionSet().contains(tokenType)) {
      parseAnonymousBlockStmt();
    } else if (ASTFunctionCallNode.getSelectionSet().contains(tokenType)) {
      parseFctCall();
    }

    exitNode(node);
  }

  private void parseVarDeclStmt() {
    ASTVarDeclNode node = new ASTVarDeclNode();
    enterNode(node);

    parseType();
    node.setVariableName(lexer.getToken().getText());
    lexer.expect(TokenType.TOK_IDENTIFIER);
    lexer.expect(TokenType.TOK_ASSIGN);
    parseTernaryExpr();
    lexer.expect(TokenType.TOK_SEMICOLON);

    exitNode(node);
  }

  private void parseAssignStmt() {
    ASTAssignStmtNode node = new ASTAssignStmtNode();
    enterNode(node);

    parseAssignExpr();
    lexer.expect(TokenType.TOK_SEMICOLON);

    exitNode(node);
  }

  private void parseAssignExpr() {
    ASTAssignExprNode node = new ASTAssignExprNode();
    enterNode(node);

    if (lexer.getToken().getType() == TokenType.TOK_IDENTIFIER) {
      node.setAssignment(true);
      node.setVariableName(lexer.getToken().getText());
      lexer.expect(TokenType.TOK_IDENTIFIER);
      lexer.expect(TokenType.TOK_ASSIGN);
    }
    parseTernaryExpr();

    exitNode(node);
  }

  private void parsePrintBuiltinCall() {
    ASTPrintBuiltinCallNode node = new ASTPrintBuiltinCallNode();
    enterNode(node);

    lexer.expect(TokenType.TOK_PRINT);
    lexer.expect(TokenType.TOK_LPAREN);
    parseTernaryExpr();
    lexer.expect(TokenType.TOK_RPAREN);

    exitNode(node);
  }

  private void parseLiteral() {
    ASTLiteralNode node = new ASTLiteralNode();
    enterNode(node);

    TokenType tokenType = lexer.getToken().getType();
    if (tokenType == TokenType.TOK_INT_LIT) {
      node.setLiteralType(ASTLiteralNode.LiteralType.INT);
      node.setLiteralValue(lexer.getToken().getText());
    } else if (tokenType == TokenType.TOK_DOUBLE_LIT) {
      node.setLiteralType(ASTLiteralNode.LiteralType.DOUBLE);
      node.setLiteralValue(lexer.getToken().getText());
    } else if (tokenType == TokenType.TOK_STRING_LIT) {
      node.setLiteralType(ASTLiteralNode.LiteralType.STRING);
      node.setLiteralValue(lexer.getToken().getText().substring(1, lexer.getToken().getText().length() - 1));
    } else if (tokenType == TokenType.TOK_TRUE) {
      node.setLiteralType(ASTLiteralNode.LiteralType.BOOL);
      node.setLiteralValue("true");
    } else if (tokenType == TokenType.TOK_FALSE) {
      node.setLiteralType(ASTLiteralNode.LiteralType.BOOL);
      node.setLiteralValue("false");
    }
    lexer.advance();

    exitNode(node);
  }

  private void parseType() {
    ASTTypeNode node = new ASTTypeNode();
    enterNode(node);

    TokenType tokenType = lexer.getToken().getType();
    if (tokenType == TokenType.TOK_TYPE_INT) {
      node.setDataType(ASTTypeNode.DataType.INT);
    } else if (tokenType == TokenType.TOK_TYPE_DOUBLE) {
      node.setDataType(ASTTypeNode.DataType.DOUBLE);
    } else if (tokenType == TokenType.TOK_TYPE_STRING) {
      node.setDataType(ASTTypeNode.DataType.STRING);
    } else if (tokenType == TokenType.TOK_TYPE_BOOL) {
      node.setDataType(ASTTypeNode.DataType.BOOL);
    } else {
      throw new RuntimeException("Unexpected token type: " + tokenType);
    }
    lexer.expect(tokenType);

    exitNode(node);
  }

  private void parseIfStmt() {
    ASTIfStmtNode node = new ASTIfStmtNode();
    enterNode(node);

    lexer.expect(TokenType.TOK_IF);
    lexer.expect(TokenType.TOK_LPAREN);
    parseTernaryExpr();
    lexer.expect(TokenType.TOK_RPAREN);
    parseIfBody();

    if (ASTElseStmtNode.getSelectionSet().contains(lexer.getToken().getType())) {
      parseElseStmt();
    }

    exitNode(node);
  }

  private void parseIfBody() {
    ASTIfBodyNode node = new ASTIfBodyNode();
    enterNode(node);

    lexer.expect(TokenType.TOK_LBRACE);
    parseStmtLst();
    lexer.expect(TokenType.TOK_RBRACE);

    exitNode(node);
  }

  private void parseElseStmt() {
    ASTElseStmtNode node = new ASTElseStmtNode();
    enterNode(node);

    lexer.expect(TokenType.TOK_ELSE);

    TokenType tokenType = lexer.getToken().getType();
    if (ASTIfStmtNode.getSelectionSet().contains(tokenType)) {
      node.setContainsIfStmt(true);
      parseIfStmt();
    } else if (ASTIfBodyNode.getSelectionSet().contains(tokenType)) {
      parseIfBody();
    } else {
      throw new RuntimeException("Unexpected token type: " + tokenType);
    }

    exitNode(node);
  }

  private void parseWhileLoopStmt() {
    ASTWhileLoopNode node = new ASTWhileLoopNode();
    enterNode(node);
    lexer.expect(TokenType.TOK_WHILE);
    lexer.expect(TokenType.TOK_LPAREN);
    parseTernaryExpr();
    lexer.expect(TokenType.TOK_RPAREN);
    lexer.expect(TokenType.TOK_LBRACE);
    parseStmtLst();
    lexer.expect(TokenType.TOK_RBRACE);

    exitNode(node);
  }

  private void parseDoWhileLoop() {
    ASTDoWhileLoopNode node = new ASTDoWhileLoopNode();
    enterNode(node);

    lexer.expect(TokenType.TOK_DO);
    lexer.expect(TokenType.TOK_LBRACE);
    parseStmtLst();
    lexer.expect(TokenType.TOK_RBRACE);
    lexer.expect(TokenType.TOK_WHILE);
    lexer.expect(TokenType.TOK_LPAREN);
    parseTernaryExpr();
    lexer.expect(TokenType.TOK_RPAREN);
    lexer.expect(TokenType.TOK_SEMICOLON);

    exitNode(node);
  }

  private void parseForLoop() {
    ASTForLoopNode node = new ASTForLoopNode();
    enterNode(node);

    lexer.expect(TokenType.TOK_FOR);
    lexer.expect(TokenType.TOK_LPAREN);
    parseVarDeclStmt();
    parseTernaryExpr();
    lexer.expect(TokenType.TOK_SEMICOLON);
    parseAssignExpr();
    lexer.expect(TokenType.TOK_RPAREN);
    lexer.expect(TokenType.TOK_LBRACE);
    parseStmtLst();
    lexer.expect(TokenType.TOK_RBRACE);

    exitNode(node);
  }

  private void parseSwitchCaseStmt() {
    ASTSwitchCaseStmtNode node = new ASTSwitchCaseStmtNode();
    enterNode(node);

    lexer.expect(TokenType.TOK_SWITCH);
    lexer.expect(TokenType.TOK_LPAREN);
    parseTernaryExpr();
    lexer.expect(TokenType.TOK_RPAREN);
    lexer.expect(TokenType.TOK_LBRACE);

    do {
      parseCaseBlock();
    } while (lexer.getToken().getType() == TokenType.TOK_CASE);

    if (lexer.getToken().getType() == TokenType.TOK_DEFAULT) {
      parseDefaultBlock();
    }

    lexer.expect(TokenType.TOK_RBRACE);

    exitNode(node);
  }

  private void parseCaseBlock() {
    ASTCaseBlockNode node = new ASTCaseBlockNode();
    enterNode(node);

    lexer.expect(TokenType.TOK_CASE);
    parseLiteral();
    lexer.expect(TokenType.TOK_COLON);
    parseStmtLst();

    exitNode(node);
  }

  private void parseDefaultBlock() {
    ASTDefaultBlockNode node = new ASTDefaultBlockNode();
    enterNode(node);

    lexer.expect(TokenType.TOK_DEFAULT);
    lexer.expect(TokenType.TOK_COLON);
    parseStmtLst();

    exitNode(node);
  }

  private void parseAnonymousBlockStmt() {
    ASTAnonymousBlockStmtNode node = new ASTAnonymousBlockStmtNode();
    enterNode(node);

    lexer.expect(TokenType.TOK_LBRACE);
    parseStmtLst();
    lexer.expect(TokenType.TOK_RBRACE);

    exitNode(node);
  }

  private void parseTernaryExpr() {
    ASTTernaryExprNode node = new ASTTernaryExprNode();
    enterNode(node);

    parseEqualityExpr();

    TokenType tokenType = lexer.getToken().getType();
    if (tokenType == TokenType.TOK_QUESTION_MARK) {
      node.setExpanded(true);

      lexer.expect(TokenType.TOK_QUESTION_MARK);
      parseEqualityExpr();

      lexer.expect(TokenType.TOK_COLON);
      parseEqualityExpr();
    }

    exitNode(node);
  }

  private void parseEqualityExpr() {
    ASTEqualityExprNode node = new ASTEqualityExprNode();
    enterNode(node);

    parseAdditiveExpr();

    TokenType tokenType = lexer.getToken().getType();
    if (tokenType == TokenType.TOK_EQUAL || tokenType == TokenType.TOK_NOT_EQUAL) {
      node.setOp(tokenType == TokenType.TOK_EQUAL ? ASTEqualityExprNode.EqualityOp.EQ : ASTEqualityExprNode.EqualityOp.NEQ);
      lexer.expectOneOf(Set.of(TokenType.TOK_EQUAL, TokenType.TOK_NOT_EQUAL));
      parseAdditiveExpr();
    }

    exitNode(node);
  }

  private void parseAdditiveExpr() {
    ASTAdditiveExprNode node = new ASTAdditiveExprNode();
    enterNode(node);

    parseMultiplicativeExpr();

    while (Set.of(TokenType.TOK_PLUS, TokenType.TOK_MINUS).contains(lexer.getToken().getType())) {
      node.addOp(lexer.getToken().getType() == TokenType.TOK_PLUS ? ASTAdditiveExprNode.AdditiveOp.PLUS : ASTAdditiveExprNode.AdditiveOp.MINUS);
      lexer.expectOneOf(Set.of(TokenType.TOK_PLUS, TokenType.TOK_MINUS));
      parseMultiplicativeExpr();
    }

    exitNode(node);
  }

  private void parseMultiplicativeExpr() {
    ASTMultiplicativeExprNode node = new ASTMultiplicativeExprNode();
    enterNode(node);

    parseAtomicExpr();

    while (Set.of(TokenType.TOK_MUL, TokenType.TOK_DIV).contains(lexer.getToken().getType())) {
      node.addOp(lexer.getToken().getType() == TokenType.TOK_MUL ? ASTMultiplicativeExprNode.MultiplicativeOp.MUL : ASTMultiplicativeExprNode.MultiplicativeOp.DIV);
      lexer.expectOneOf(Set.of(TokenType.TOK_MUL, TokenType.TOK_DIV));
      parseAtomicExpr();
    }

    exitNode(node);
  }

  private void parseAtomicExpr() {
    ASTAtomicExprNode node = new ASTAtomicExprNode();
    enterNode(node);

    TokenType tokenType = lexer.getToken().getType();
    if (ASTLiteralNode.getSelectionSet().contains(tokenType)) {
      parseLiteral();
    } else if (ASTFunctionCallNode.getSelectionSet().contains(tokenType)) {
      parseFctCall();
    } else if (ASTPrintBuiltinCallNode.getSelectionSet().contains(tokenType)) {
      parsePrintBuiltinCall();
    } else if (tokenType == TokenType.TOK_IDENTIFIER) {
      node.setVariableName(lexer.getToken().getText());
      lexer.expect(TokenType.TOK_IDENTIFIER);
    } else if (tokenType == TokenType.TOK_LPAREN) {
      lexer.expect(TokenType.TOK_LPAREN);
      parseTernaryExpr();
      lexer.expect(TokenType.TOK_RPAREN);
    }

    exitNode(node);
  }

  // ------------------ AST node helpers ------------------

  private void enterNode(ASTNode node) {
    // Attach CodeLoc to AST node
    node.setCodeLoc(lexer.getToken().getCodeLoc());

    if (!parentStack.empty()) {
      // Make sure the node is not pushed twice
      assert parentStack.peek() != node;
      // Link parent and child nodes so that we can traverse the tree
      ASTNode parent = parentStack.peek();
      parent.addChild(node);
      node.setParent(parent);
    }
    // Push the node onto the stack
    parentStack.push(node);
  }

  private void exitNode(ASTNode node) {
    // Make sure the node is the last one pushed
    assert !parentStack.empty();
    assert parentStack.peek() == node;
    // Remove the node from the stack
    parentStack.pop();
  }

}
