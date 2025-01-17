(ns clojure.platform
  (:require [clojure.string :as str])
  (:import (com.thoughtworks.qdox JavaProjectBuilder)))

(defn find-duplicates [coll]
  (->> coll
       frequencies
       (filter (fn [[_ count]] (> count 1)))
       (map first)))

(defn handle-is? [s]
  (if (re-find #"^is-" s)
    (-> s
        (str/replace #"^is-" "")
        (str "?"))
    s))

(defn- clojurize-name [name]
  (-> name
      (str/replace #"^get" "")
      (str/replace #"^new" "")
      (str/replace #"([A-Z])" "-$1") ; convert camelCase to kebab-case
      (str/lower-case)
      (str/replace #"^-" "")
      handle-is?))

(defn test-clojurize-name []
  (assert (= (clojurize-name "getFileHandle") "file-handle"))
  (assert (= (clojurize-name "isLocalStorageAvailable") "local-storage-available?"))
  (assert (= (clojurize-name "newFooBaz") "foo-baz"))
  (assert (= (clojurize-name "getFilePath") "file-path"))
  (assert (= (clojurize-name "isLoggedIn") "logged-in?"))
  (assert (= (clojurize-name "newUser") "user"))
  (assert (= (clojurize-name "getUserProfile") "user-profile"))
  (assert (= (clojurize-name "getFileHandle") "file-handle"))
  (assert (= (clojurize-name "isAdmin") "admin?"))
  (assert (= (clojurize-name "newItem") "item"))
  (println "All tests passed!"))

(test-clojurize-name)

(defn parse-class-methods [builder class]
  (->> (.getMethods (.getClassByName builder class))
       (map (fn [method]
              {:method method
               :name         (.getName method)
               :return-type  (.getReturnType method)
               :parameters   (->> (.getParameters method)
                                  (map (fn [param]
                                         {:name (.getName param)
                                          :type (.getType param)})))
               :javadoc      (some-> method .getComment)
               :annotations  (->> (.getAnnotations method)
                                  (map #(.getTypeName %)))
               :code-block   (.getCodeBlock method)
               :is-static    (.isStatic method)
               :is-abstract  (.isAbstract method)}))))

(defn java-src-folder->parse-methods [folder interfaces]
  (let [builder (doto (JavaProjectBuilder.)
                  (.addSourceFolder (java.io.File. folder)))]
    (into {}
          (for [interface interfaces]
            [interface (parse-class-methods builder interface)]))))

(comment

 (def jpbuilder (doto (JavaProjectBuilder.)
                  (.addSourceTree (java.io.File. "gdx-src/"))))

 (:code-block (first (parse-class-methods jpbuilder "com.badlogic.gdx.Application")))

 (.getAnnotations (clojure.pprint/pprint
                   (bean (first (.getMethods (.getClassByName jpbuilder "com.badlogic.gdx.Application"))))))

 (count (.getClasses jpbuilder))
 ; 942

 (count (mapcat #(.getMethods %) (.getClasses jpbuilder)))
 ; 10540

 (count (distinct (map #(.getName %) (mapcat #(.getMethods %) (.getClasses jpbuilder)))))
 ; 3534

(find-duplicates (map #(.getName %) (mapcat #(.getMethods %) (.getClasses jpbuilder))))


 )

(comment

 ; its in the names - what are all unique names
 ; just make them as functions in 1 namespace
 ; clojure.x ?
 ; Count distinct

 (def jpbuilder (doto (JavaProjectBuilder.)
                  (.addSourceTree (java.io.File. "gdx-src/"))))

 (clojure.pprint/pprint
  (sort (map #(.getName %) (.getPackages jpbuilder))))

 (map #(.getName %) (.getMethods (first (.getClasses jpbuilder))))

(clojure.pprint/pprint
  (sort (map #(.getName %) (.getPackages jpbuilder))))
 (first (.getMethods (.getClassByName jpbuilder "com.badlogic.gdx.Gdx")))
 ; strange the thing is not properly searching
 )

(def first-parameter? true)

; TODO why distinct?!?!
(defn- ->parameters-str [parameters]
  (str "["
       (str/join " " (cons (if first-parameter?  "_" "")
                           (map (comp clojurize-name :name) parameters)))
       "]"))

(defn- ->parameters [parameters]
  (if (empty? parameters)
    ""
    (str/join " " (map (comp clojurize-name :name) parameters))))

(defn ->protocol-function-string [{:keys [name parameters javadoc code-block]}]
  (str "  (" (clojurize-name name) " " (->parameters-str parameters) "\n    \"" javadoc "\n\n" code-block "\")"))

; :arglists '([xform* coll])
(defn ->declare-form [{:keys [name parameters javadoc]}]
  (str
   "(def ^{:doc \"" javadoc "\" "
   ":arglists '(" (->parameters-str parameters) ")} "
   (clojurize-name name)
   ")"))

(defn ->function [{:keys [name parameters javadoc]}]
  (str
   "(defn " (clojurize-name name) "\n"
   "  \"" javadoc "\"\n"
   "  " (->parameters-str parameters) "\n"
   "  (." name  " Gdx/foo" (if (seq parameters) " ") (->parameters parameters) "))"))

(defn- protocol-str [name content]
  (str "(defprotocol " name "\n" content ")"))

(defn- namespace-line-string [name]
  (str "(ns " ns-name ")\n\n")) ; TODO we pass lines,?

(defn generate-namespace [output ns-name interface-data file class-str]
  (let [duplicates (find-duplicates (map :name (get interface-data class-str)))]
    ;;

    ; FIXME handle duplicates
    (when (seq duplicates)
      (println "duplicates at " class-str ": " duplicates ", filtering those."))
    ;;

    (spit file
          (str (namespace-line-string ns-name)
               (case output
                 :protocol (protocol-str (str/capitalize ns-name)
                                         (str/join "\n\n"
                                                   (map ->protocol-function-string (get interface-data class-str))))
                 :declares (str/join "\n\n" (map ->declare-form (get interface-data class-str)))
                 :functions (str/join "\n\n" (map ->function (get interface-data class-str)))
                 )))))

(defn generate-namespaces [src-folder folder output]
  {:pre [(.exists (java.io.File. folder))]}
  (let [interfaces ["com.badlogic.gdx.Application"
                    "com.badlogic.gdx.Audio"
                    "com.badlogic.gdx.Files"
                    "com.badlogic.gdx.graphics.GL20"
                    "com.badlogic.gdx.graphics.GL30"
                    "com.badlogic.gdx.graphics.GL31"
                    "com.badlogic.gdx.graphics.GL32"
                    "com.badlogic.gdx.Graphics"
                    "com.badlogic.gdx.Input"
                    "com.badlogic.gdx.Net"]
        interface-data (java-src-folder->parse-methods src-folder interfaces)]
    (def last-interface-data interface-data)
    (println "Parsed src folder, result: ")
    (doseq [[n mths] interface-data]
      (println n ":" (count mths) " methods."))
    (println)
    (doseq [[file class] {"application" "com.badlogic.gdx.Application"
                          "audio"       "com.badlogic.gdx.Audio"
                          "files"       "com.badlogic.gdx.Files"
                          "graphics"    "com.badlogic.gdx.Graphics"
                          "gl20"        "com.badlogic.gdx.graphics.GL20"
                          "gl30"        "com.badlogic.gdx.graphics.GL30"
                          "gl31"        "com.badlogic.gdx.graphics.GL31"
                          "gl32"        "com.badlogic.gdx.graphics.GL32"
                          "input"       "com.badlogic.gdx.Input"
                          "net"         "com.badlogic.gdx.Net"}
            :let [target-file (str folder file ".clj")]]
      (generate-namespace output
                          (str "clojure." file)
                          interface-data
                          target-file
                          class)
      (println target-file"\n"))))

(defn generate-core-namespace [ns-name]
  [(namespace-line-string ns-name)]
  )

(generate-core-namespace "clojure")

(comment
 ; 1. copy from libgdx directory core gdx sources
 ; cp -r ~/projects/libgdx/gdx/src/ gdx-src/
 ; It needs to be the source directory itself
 ; and not a super-directory otherwise params are broken (p0, p1, ..)

 ; 2. create target dir
 ; `mkdir generate/`
 (generate-namespaces "gdx-src/" ;
                      "generate/"
                      :protocol ;:declares ; :protocol
                      ) ; make dir to put results


 ; TODO comments have to escape '\"'
 ; TODO missing javadoc :
 ; /** @return the average number of frames per second */

 ; interface-data = coll of methods
 ; =. tests
 (clojure.pprint/pprint
  (first (get last-interface-data "com.badlogic.gdx.Application")))
 (->function (first (get last-interface-data "com.badlogic.gdx.Application")))
 (->parameters (:parameters (first (get last-interface-data "com.badlogic.gdx.Application"))))

 ; or just declare them - they are not thread-bound !?

 (declare ^:dynamic *application*
          ^:dynamic *audio ; not using
          ^:dynamic *files* ; using 3 times
          ^:dynamic *graphics* ; using 1 time

          ; those are in graphics
          ^:dynamic *gl20*
          ^:dynamic *gl30*
          ^:dynamic *gl31*
          ^:dynamic *gl32*

          ^:dynamic *input*
          ^:dynamic *net*)

 ; TODO - implementations -

 ; (.bindRoot #'clojure.application/type (fn [] (Application/.getType *application*)))


 ; TODO also Sound/Music/etc. ? other interfaces?
 ; normal classes wrap too ? could make a full wrapper?
 ; but why ?

 ; # Tools

 ; ## Find all uses of 'Gdx.' global state:
 ; :vimgrep/Gdx\./g gdx/**/*.java

 ; => add them all to 'clojure.core' fork -
 ; so can call directly key-pressed? or exit ?
 ; - that would be the next step -
 ; or with prefix
 ; app-exit
 ; app-post-runnable
 ; input-x
 ; input-y
 ; gl-foobar
 ; new-sound
 )
