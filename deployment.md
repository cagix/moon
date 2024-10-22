## Instructions

1. save assets as edn and load them via this fn:
    (cannot do list/search files in jar in same way)

```clojure
(comment

 (defn- assets []
   (for [[file class-str] (clojure.edn/read-string (slurp (clojure.java.io/resource "assets.edn")))]
     [file (case class-str
             "com.badlogic.gdx.audio.Sound" Sound
             "com.badlogic.gdx.graphics.Texture" Texture)]))

 (spit "resources/assets.edn"
       (utils.core/->edn-str (map (fn [[file class]]
                                    [file (.getName class)])
                                  (doall (search-assets "resources/")))))

 )
```

* Add (:gen-class) to core.app
* `lein uberjar`
* `cd target/uberjar`
* `java -jar vampire.jar`

(Start it not from game folder to see if assets r working)
