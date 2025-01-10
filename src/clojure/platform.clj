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
               :is-static    (.isStatic method)
               :is-abstract  (.isAbstract method)}))))

(defn java-src-folder->parse-methods [folder interfaces]
  (let [builder (doto (JavaProjectBuilder.)
                  (.addSourceFolder (java.io.File. folder)))]
    (into {}
          (for [interface interfaces]
            [interface (parse-class-methods builder interface)]))))

; TODO why distinct?!?!
(defn- ->parameters-str [parameters]
  (str/join " " (distinct (map (comp clojurize-name :name) parameters))))

(defn ->protocol-function-string [{:keys [name parameters javadoc]}]
  (str "  (" (clojurize-name name) " [_ " (->parameters-str parameters) "]\n    \"" javadoc "\")"))

; :arglists '([xform* coll])
(defn ->declare-form [{:keys [name parameters javadoc]}]
  (str
   "(def ^{:doc \"" javadoc "\" "
   ":arglists '([_ " (->parameters-str parameters) "])} "
   (clojurize-name name)
   ")"))

(defn generate-namespace [ns-name interface-data file class-str]
  (let [duplicates (find-duplicates (map :name (get interface-data class-str)))]
    ; FIXME handle duplicates
    (when (seq duplicates)
      (println "duplicates at " class-str ": " duplicates ", filtering those."))
    (spit file
          (str "(ns clojure." ns-name ")\n\n"
               ;"(defprotocol " (str/capitalize ns-name) "\n"
               (str/join "\n\n" #_(map ->protocol-function-string (get interface-data class-str))
                         (map ->declare-form (get interface-data class-str))
                         )
               ;"\n)"
               ))))

(defn generate-namespaces [src-folder folder]
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
      (generate-namespace file
                          interface-data
                          target-file
                          class)
      (println target-file"\n"))))

(comment
 (generate-namespaces "gdx/" ; copy from libgdx directory core gdx sources
                      "generate/") ; make dir to put results

 ; TODO p0 as args instead of real names, idk why
 ; ... gdx/ folder solved that


 ; TODO also Sound/Music/etc. ? other interfaces?
 ; normal classes wrap too ? could make a full wrapper?
 ; but why ?

 ; # Tools

 ; ## Find all uses of 'Gdx.' global state:
 ; :vimgrep/Gdx\./g gdx/**/*.java
 )
