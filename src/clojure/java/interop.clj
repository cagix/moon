(ns clojure.java.interop)

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
   (into {}
         (for [field (relevant-fields class-str field-type)]
           [(keyword (->clojure-symbol field))
            (symbol class-str (str (:name field)))])))

 (= (create-mapping "Input$Buttons" 'int)
    {:forward Input$Buttons/FORWARD,
     :left Input$Buttons/LEFT,
     :back Input$Buttons/BACK,
     :right Input$Buttons/RIGHT,
     :middle Input$Buttons/MIDDLE}
    )

 (= (apply concat (create-mapping "Input$Buttons" 'int))
    (apply concat {:forward Input$Buttons/FORWARD,
     :left Input$Buttons/LEFT,
     :back Input$Buttons/BACK,
     :right Input$Buttons/RIGHT,
     :middle Input$Buttons/MIDDLE})
    )

 (defn generate-mapping [class-str field-type]
   (spit "temp.clj"
         (with-out-str
          (println "{")
          (doseq [[kw static-field] (sort-by first (create-mapping class-str field-type))]
            (println kw static-field))
          (println "}"))))

 (generate-mapping "Input$Buttons" 'int)
 (generate-mapping "Input$Keys"    'int) ; without Input$Keys/MAX_KEYCODE
 (generate-mapping "Color" 'com.badlogic.gdx.graphics.Color)

 (import 'com.badlogic.gdx.utils.Align)
 (generate-mapping "Align" 'int)

 (create-mapping "Align" 'int)

 )
