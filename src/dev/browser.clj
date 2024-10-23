(ns dev.browser
  (:require [clojure.java.io :as io]
            [dev.javafx :as fx])
  (:import (javafx.event EventHandler)
           (javafx.scene.control Button TreeItem TreeView)
           (javafx.scene.image Image ImageView)
           (javafx.scene.layout StackPane)
           (javafx.scene Scene Node))
  #_(:gen-class :extends javafx.application.Application))

; Project management tool!
; Write for a ns if stable/unstable/see docs etc

; Actually we do _not_ need to see private stuff!
; or ns form!
; just publics -> generate documentation ...

(comment

 ; * remove comment at :gen-class
 ; * lein with-profile javafx repl
 ; * lein clean before doing `dev` again (ns refresh doesnt work with aot)
 (fx/init)
 (fx/run (tool-tree "src/"))

 (require '[rewrite-clj.parser :as p])
 (p/parse-file-all "src/core/stat.clj")

 )

(defn -start [app stage]
  (def stage stage))

(defn- clj-file-forms [file]
  (read-string (str "[" (slurp file) "]")))

(comment
 (first (doall (map (partial take 2) (clj-file-forms "src/gdx.clj"))))

 )

(declare stage)

(defn amount-syms [sym]
  (case (str sym)
    ("ns" "def" "declare" "defmacro" "defn" "defn-" "defmulti"
          "defsystem" "defc" "defrecord" "defprotocol" "deftype") 1
    ("defmethod" "derive") 2
    0))

(defn- first-sym->icon [sym]
  (case (str sym)
    "ns"          "📖"
    "comment"     "💬"
    "def"         "📌"
    "declare"     "📌"
    "defmacro"    "🏗️"
    "defn"        "🔧"
    "defn-"       "🔒🔧"
    "defmulti"    "🔧️"
    "defsystem"   "🔧️️"
    "defc"        "🧩"
    "defmethod"   "🧩"
    "derive"      "🧬"
    "defprotocol" "📄🔧️️"
    "defrecord"   "🗃️"
    "deftype"     "📦"
    "?"))

(require '[clojure.string :as str])

(defn- form->label [[first-sym & more]]
  (str (first-sym->icon first-sym)
       " "
       first-sym
       " "
       (when-let [amount (amount-syms first-sym)]
         (str/join " " (take amount more)))))

(defn- form->rest [[first-sym & more]]
  (apply str (if-let [amount (amount-syms first-sym)]
               (drop amount more)
               more)))

(defn- clj-files [folder]
  (sort (filter #(str/ends-with? % ".clj") (map str (file-seq (io/file "src/"))))))

; (clj-files "src/")

(defn- path->ns-str [path]
  (-> path
      (str/replace "src/" "")
      (str/replace ".clj" "")
      (str/replace "/" ".")
      (str/replace "_" ".")))

(defn- file->pretty [file]
  (let [ns-str (path->ns-str file)]
    (str (when-let [icon (::icon (meta (find-ns (symbol ns-str))))]
           (str icon " "))
         ns-str)))

; (meta (find-ns (symbol (file->pretty "src/gdx/app.clj"))))

(defn- form->node [form]
  (let [tree-item (TreeItem. (form->label form))]
    #_(cond (= 'defprotocol (first form))
          (do
           (println "Found defprotocol : " (second form))
           (doseq [sig (sort (map name (keys (:sigs gdx.app/Listener))))]
             (.add (.getChildren tree-item) (TreeItem. (str (first-sym->icon 'defn) sig))))))
    (.add (.getChildren tree-item) (TreeItem. (form->rest form)))
    tree-item))

(defn- file-forms-tree-item [file]
  (println "file-forms-tree-item . file: " file " - pretty: " (file->pretty file))
  (let [item (TreeItem. (file->pretty file))]
    (doseq [form (clj-file-forms file)]
      (.add (.getChildren item) (form->node form)))
    item))

;(.getName (.isDirectory (second (seq (.listFiles (io/file "src/"))))))
(defn- directory->pretty-name [path]
  (case path
    "app"       "🖥️ app"
    "component" "⚙️ component"
    "data"      "💿 data"
    "dev"       "💼 dev"
    "editor"    "🖋️ editor"
    "gdx"       "🎮 gdx"
    "level"     "🗺️ level"
    "utils"     "🧰 utils"
    "world"     "🌍 world"
    path))

(defn add-items! [root-item root-file]
  (doseq [file (sort-by java.io.File/.getName (seq (.listFiles root-file)))
          :let [file-name (.getName file)
                path (.getPath file)]
          :when (not (str/starts-with? file-name "."))]
    (.add (.getChildren root-item)
          (cond
           (.isDirectory file)
           (let [tree-item (TreeItem. (directory->pretty-name (path->ns-str path)))]
             (add-items! tree-item file)
             tree-item)

           (str/ends-with? file-name ".clj")
           (file-forms-tree-item path)

           :else
           (TreeItem. path)))))

(defn tool-tree [folder]
  (.setTitle stage "Tool")
  (let [root-item (TreeItem. folder)]
    (.setExpanded root-item true)
    (add-items! root-item (io/file "src/"))
    (let [stack-pane (StackPane.)]
      (.add (.getChildren stack-pane) (TreeView. root-item))
      (let [scene (Scene. stack-pane 400 900)]
        (.add (.getStylesheets scene) (.toExternalForm (io/resource "darkmode.css")))
        (.setScene stage scene))
      (.show stage))))
