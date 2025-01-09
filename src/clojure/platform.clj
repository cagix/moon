(ns clojure.platform
  (:require [clojure.string :as str])
  (:import (com.thoughtworks.qdox JavaProjectBuilder)))

(defn find-duplicates [coll]
  (->> coll
       frequencies
       (filter (fn [[_ count]] (> count 1))) ; Find elements with a count > 1
       (map first)))                        ; Extract the duplicate keys

; (apply distinct? (apply concat (map second interface-data)))

(defn- clojurize-name [name]
  (-> name
      (str/replace #"^get" "")
      (str/replace #"([A-Z])" "")
      (str/lower-case)
      (str/replace #"^-+" ""))) ; ???

(defn parse-class-methods [builder class]
  (->> (.getMethods (.getClassByName builder class))
       (map (fn [method]
              {:name         (.getName method)
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

(defn generate-data [folder interfaces]
  (let [builder (doto (JavaProjectBuilder.)
                  (.addSourceFolder (java.io.File. folder)))]
    (into {}
          (for [interface interfaces]
            [interface (parse-class-methods builder interface)]))))

(comment

 (def interface-data (generate-data "gdx/"
                                    ["com.badlogic.gdx.Application"
                                     "com.badlogic.gdx.Audio"
                                     "com.badlogic.gdx.Files"
                                     "com.badlogic.gdx.graphics.GL20"
                                     "com.badlogic.gdx.graphics.GL30"
                                     "com.badlogic.gdx.graphics.GL31"
                                     "com.badlogic.gdx.graphics.GL32"
                                     "com.badlogic.gdx.Graphics"
                                     "com.badlogic.gdx.Input"
                                     "com.badlogic.gdx.Net"]))

 ; (comp clojurize-name :name)

 (clojure.pprint/pprint
  (map (fn [[n mths]] [n (count mths)]) interface-data))
 (["com.badlogic.gdx.Application" 26]
  ["com.badlogic.gdx.Audio" 6]
  ["com.badlogic.gdx.Files" 10]
  ["com.badlogic.gdx.graphics.GL20" 162]
  ["com.badlogic.gdx.graphics.GL30" 96]
  ["com.badlogic.gdx.graphics.GL31" 68]
  ["com.badlogic.gdx.graphics.GL32" 42]
  ["com.badlogic.gdx.Graphics" 55]
  ["com.badlogic.gdx.Input" 50]
  ["com.badlogic.gdx.Net" 7])
 )


