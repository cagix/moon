(ns clojure.gdx.interop
  (:require [clojure.gdx.input.buttons :as input.buttons]
            [clojure.gdx.input.keys :as input.keys]
            [clojure.gdx.utils.align :as align]))

(defn- static-field [mapping exception-name k]
  (when-not (contains? mapping k)
    (throw (IllegalArgumentException. (str "Unknown " exception-name ": " k ". \nOptions are:\n" (sort (keys mapping))))))
  (k mapping))

(def k->input-button (partial static-field input.buttons/mapping "Button"))
(def k->input-key    (partial static-field input.keys/mapping    "Key"))
(def k->align        (partial static-field align/mapping         "Align"))

(comment

 (require '[clojure.string :as str]
          '[clojure.reflect :refer [type-reflect]])

 (defn- relevant-fields [class-str field-type]
   (->> class-str
        symbol
        eval
        type-reflect
        :members
        (filter #(= field-type (:type %)))))

 (defn- ->clojure-symbol [field]
   (-> field :name name str/lower-case (str/replace #"_" "-") symbol))

 (defn create-mapping [class-str field-type]
   (sort-by first
            (for [field (relevant-fields class-str field-type)]
              [(keyword (->clojure-symbol field))
               (symbol class-str (str (:name field)))])))

 (defn generate-mapping [class-str field-type]
   (spit "temp.clj"
         (with-out-str
          (println "{")
          (doseq [[kw static-field] (create-mapping class-str field-type)]
            (println kw static-field))
          (println "}"))))

 (generate-mapping "Input$Buttons" 'int)
 (generate-mapping "Input$Keys"    'int) ; without Input$Keys/MAX_KEYCODE
 (generate-mapping "Color" 'com.badlogic.gdx.graphics.Color)

 (import 'com.badlogic.gdx.utils.Align)
 (generate-mapping "Align" 'int)

 )
