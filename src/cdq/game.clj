(ns cdq.game
  (:require [clojure.gdx.backends.lwjgl :as lwjgl]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.walk :as walk])
  (:import (com.badlogic.gdx ApplicationAdapter)))

(defn- require-symbols [form]
  (walk/postwalk (fn [form]
                   (if (symbol? form)
                     (if (namespace form)
                       (requiring-resolve form)
                       (do (require form) form))
                     form))
                 form))

(defn- create-config [path]
  (let [m (->> path
               io/resource
               slurp
               edn/read-string
               require-symbols)]
    (reify clojure.lang.ILookup
      (valAt [_ k]
        (assert (contains? m k)
                (str "Config key not found: " k))
        (get m k)))))

(def state (atom nil))

(defn -main [config-path]
  (let [config (create-config config-path)]
    (lwjgl/application (:lwjgl-application config)
                       (proxy [ApplicationAdapter] []
                         (create []
                           (reset! state ((:create! config) config)))

                         (dispose []
                           ((:dispose! config) @state))

                         (render []
                           (swap! state (:render! config)))

                         (resize [width height]
                           ((:resize! config) @state width height))))))
