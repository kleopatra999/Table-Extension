package org.nlogo.extensions.table;

import org.nlogo.core.CompilerException;
import org.nlogo.core.LogoList;
import org.nlogo.core.Syntax;
import org.nlogo.core.SyntaxJ;
import org.nlogo.api.LogoException;
import org.nlogo.api.LogoListBuilder;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.Argument;
import org.nlogo.api.Context;
import org.nlogo.api.Reporter;
import org.nlogo.api.Command;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class TableExtension
    extends org.nlogo.api.DefaultClassManager {

  public void load(org.nlogo.api.PrimitiveManager primManager) {
    primManager.addPrimitive("clear", new Clear());
    primManager.addPrimitive("get", new Get());
    primManager.addPrimitive("has-key?", new HasKey());
    primManager.addPrimitive("keys", new Keys());
    primManager.addPrimitive("length", new Length());
    primManager.addPrimitive("make", new Make());
    primManager.addPrimitive("put", new Put());
    primManager.addPrimitive("remove", new Remove());
    primManager.addPrimitive("from-list", new FromList());
    primManager.addPrimitive("to-list", new ToList());
  }

  private static WeakHashMap<Table, Long> tables = new WeakHashMap<Table, Long>();

  private static long next = 0;

  ///

  // It's important that we extend LinkedHashMap here, rather than
  // plain HashMap, because we want model results to be reproducible
  // crossplatform.

  public static class Table
      extends LinkedHashMap<Object, Object>
      // new NetLogo data types defined by extensions must implement
      // this interface
      implements org.nlogo.api.ExtensionObject {
    private final long id;

    public Table() {
      tables.put(this, next);
      id = next;
      next++;
    }

    public Table(LogoList alist)
        throws org.nlogo.api.ExtensionException {
      this();
      addAll(alist);
    }

    public void addAll(LogoList alist)
        throws ExtensionException {
      for (Iterator<Object> it = alist.javaIterator(); it.hasNext();) {
        Object pair = it.next();
        if ((pair instanceof LogoList
            && ((LogoList) pair).size() < 2)
            || (!(pair instanceof LogoList))) {
          throw new org.nlogo.api.ExtensionException
              ("expected a two-element list: " +
                  org.nlogo.api.Dump.logoObject(pair));
        }
        this.put(((LogoList) pair).first(),
            ((LogoList) pair).butFirst().first());
      }
    }

    public Table(long id) {
      this.id = id;
      tables.put(this, id);
      next = StrictMath.max(next, id + 1);
    }

    public boolean equals(Object obj) {
      return this == obj;
    }

    public LogoList toList() {
      LogoListBuilder alist = new LogoListBuilder();
      for (Map.Entry<Object, Object> entry : entrySet()) {
        LogoListBuilder pair = new LogoListBuilder();
        pair.add(entry.getKey());
        pair.add(entry.getValue());
        alist.add(pair.toLogoList());
      }
      return alist.toLogoList();
    }

    public String dump(boolean readable, boolean exportable, boolean reference) {
      if (exportable && reference) {
        return ("" + id);
      } else {
        return (exportable ? (id + ": ") : "") + org.nlogo.api.Dump.logoObject(this.toList(), true, exportable);
      }
    }

    public String getExtensionName() {
      return "table";
    }

    public String getNLTypeName() {
      // since this extension only defines one type, we don't
      // need to give it a name; "table:" is enough,
      // "table:table" would be redundant
      return "";
    }

    public boolean recursivelyEqual(Object o) {
      if (!(o instanceof Table)) {
        return false;
      }
      Table otherTable = (Table) o;
      if (size() != otherTable.size()) {
        return false;
      }
      for (Iterator<Object> iter = keySet().iterator(); iter.hasNext();) {
        Object key = iter.next();
        if (!otherTable.containsKey(key)
            || !org.nlogo.api.Equality.equals(get(key),
            otherTable.get(key))) {
          return false;
        }
      }
      return true;
    }
  }

  public void clearAll() {
    tables.clear();
    next = 0;
  }

  public StringBuilder exportWorld() {
    StringBuilder buffer = new StringBuilder();
    for (Table table : tables.keySet()) {
      buffer.append
          (org.nlogo.api.Dump.csv().encode
              (org.nlogo.api.Dump.extensionObject(table, true, true, false)) + "\n");
    }
    return buffer;
  }

  public void importWorld(List<String[]> lines, org.nlogo.api.ExtensionManager reader,
                          org.nlogo.api.ImportErrorHandler handler)
      throws ExtensionException {
    for (String[] line : lines) {
      try {
        reader.readFromString(line[0]);
      } catch (CompilerException e) {
        handler.showError("Error importing arrays", e.getMessage(), "This array will be ignored");
      }
    }
  }

  ///

  public static class Clear implements Command {
    public Syntax getSyntax() {
      return SyntaxJ.commandSyntax
          (new int[]{Syntax.WildcardType()});
    }

    public void perform(Argument args[], Context context)
        throws ExtensionException, LogoException {
      Object arg0 = args[0].get();
      if (!(arg0 instanceof Table)) {
        throw new org.nlogo.api.ExtensionException
            ("not a table: " +
                org.nlogo.api.Dump.logoObject(arg0));
      }
      ((Table) arg0).clear();
    }
  }

  public static class Get implements Reporter {
    public Syntax getSyntax() {
      return SyntaxJ.reporterSyntax
          (new int[]{Syntax.WildcardType(), Syntax.WildcardType()},
              Syntax.WildcardType());
    }

    public Object report(Argument args[], Context context)
        throws ExtensionException, LogoException {
      Object arg0 = args[0].get();
      if (!(arg0 instanceof Table)) {
        throw new org.nlogo.api.ExtensionException
            ("not a table: " +
                org.nlogo.api.Dump.logoObject(arg0));
      }
      Object key = args[1].get();
      Object result = ((Table) arg0).get(key);
      if (result == null) {
        throw new ExtensionException
            ("No value for " + org.nlogo.api.Dump.logoObject(key)
                + " in table.");
      }
      return result;
    }
  }

  public static class HasKey implements Reporter {
    public Syntax getSyntax() {
      return SyntaxJ.reporterSyntax
          (new int[]{Syntax.WildcardType(), Syntax.WildcardType()},
              Syntax.BooleanType());
    }

    public Object report(Argument args[], Context context)
        throws ExtensionException, LogoException {
      Object arg0 = args[0].get();
      if (!(arg0 instanceof Table)) {
        throw new org.nlogo.api.ExtensionException
            ("not a table: " +
                org.nlogo.api.Dump.logoObject(arg0));
      }
      return Boolean.valueOf
          (((Table) arg0).containsKey(args[1].get()));
    }
  }

  public static class Keys implements Reporter {
    public Syntax getSyntax() {
      return SyntaxJ.reporterSyntax
          (new int[]{Syntax.WildcardType()},
              Syntax.ListType());
    }

    public Object report(Argument args[], Context context)
        throws ExtensionException, LogoException {
      Object arg0 = args[0].get();
      if (!(arg0 instanceof Table)) {
        throw new org.nlogo.api.ExtensionException
            ("not a table: " +
                org.nlogo.api.Dump.logoObject(arg0));
      }
      return LogoList.fromJava(((Table) arg0).keySet());
    }
  }

  public static class Length implements Reporter {
    public Syntax getSyntax() {
      return SyntaxJ.reporterSyntax
          (new int[]{Syntax.WildcardType()},
              Syntax.NumberType());
    }

    public Object report(Argument args[], Context context)
        throws ExtensionException, LogoException {
      Object arg0 = args[0].get();
      if (!(arg0 instanceof Table)) {
        throw new org.nlogo.api.ExtensionException
            ("not a table: " +
                org.nlogo.api.Dump.logoObject(arg0));
      }
      return Double.valueOf(((Table) arg0).size());
    }
  }

  public static class Make implements Reporter {
    public Syntax getSyntax() {
      return SyntaxJ.reporterSyntax(Syntax.WildcardType());
    }

    public Object report(Argument args[], Context context)
        throws ExtensionException, LogoException {
      return new Table();
    }

  }

  public static class Put implements Command {
    public Syntax getSyntax() {
      return SyntaxJ.commandSyntax
          (new int[]{Syntax.WildcardType(), Syntax.WildcardType(),
              Syntax.WildcardType()});
    }

    public void perform(Argument args[], Context context)
        throws ExtensionException, LogoException {
      Object arg0 = args[0].get();
      if (!(arg0 instanceof Table)) {
        throw new org.nlogo.api.ExtensionException
            ("not a table: " +
                org.nlogo.api.Dump.logoObject(arg0));
      }
      Object key = args[1].get();
      if (!isValidKey(key)) {
        throw new org.nlogo.api.ExtensionException
            (org.nlogo.api.Dump.logoObject(key) + " is not a valid table key "
                + "(a table key may only be a number, a string, true or false, or a list "
                + "whose items are valid keys)");
      }
      ((Table) arg0).put(key, args[2].get());
    }
  }

  public static class Remove implements Command {
    public Syntax getSyntax() {
      return SyntaxJ.commandSyntax
          (new int[]{Syntax.WildcardType(), Syntax.WildcardType()});
    }

    public void perform(Argument args[], Context context)
        throws ExtensionException, LogoException {
      Object arg0 = args[0].get();
      if (!(arg0 instanceof Table)) {
        throw new org.nlogo.api.ExtensionException
            ("not a table: " +
                org.nlogo.api.Dump.logoObject(arg0));
      }
      ((Table) arg0).remove(args[1].get());
    }
  }

  public static class ToList implements Reporter {
    public Syntax getSyntax() {
      return SyntaxJ.reporterSyntax
          (new int[]{Syntax.WildcardType()},
              Syntax.ListType());
    }

    public Object report(Argument args[], Context context)
        throws ExtensionException, LogoException {
      Object arg0 = args[0].get();
      if (!(arg0 instanceof Table)) {
        throw new org.nlogo.api.ExtensionException
            ("not a table: " +
                org.nlogo.api.Dump.logoObject(arg0));
      }
      return ((Table) arg0).toList();

    }
  }


  public static class FromList implements Reporter {
    public Syntax getSyntax() {
      return SyntaxJ.reporterSyntax
          (new int[]{Syntax.ListType()},
              Syntax.WildcardType());
    }

    public Object report(Argument args[], Context context)
        throws ExtensionException, LogoException {
      LogoList alist = args[0].getList();
      return new Table(alist);
    }
  }

  public org.nlogo.api.ExtensionObject readExtensionObject(org.nlogo.api.ExtensionManager reader,
                                                           String typeName, String value)
      throws org.nlogo.api.ExtensionException {
    try {
      String[] s = value.split(":");
      long id = Long.parseLong(s[0]);
      Table table = getOrCreateTableFromId(id);
      if (s.length > 1) {
        table.addAll((LogoList) reader.readFromString(s[1]));
      }
      return table;
    } catch (CompilerException ex) {
      throw new org.nlogo.api.ExtensionException(ex.getMessage());
    }
  }

  private Table getOrCreateTableFromId(long id) {
    for (Table table : tables.keySet()) {
      if (table.id == id) {
        return table;
      }
    }
    return new Table(id);
  }

  /// helpers

  private static boolean isValidKey(Object key) {
    return
        key instanceof Double ||
            key instanceof String ||
            key instanceof Boolean ||
            (key instanceof LogoList &&
                containsOnlyValidKeys((LogoList) key));
  }

  private static boolean containsOnlyValidKeys(LogoList list) {
    for (Iterator<Object> it = list.javaIterator(); it.hasNext();) {
      Object o = it.next();
      if (!isValidKey(o)) {
        return false;
      }
    }
    return true;
  }

}
