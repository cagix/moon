(ns cdq.application
  (:require [clojure.edn :as edn]
            [clojure.gdx.utils]
            [clojure.scene2d.stage :as stage]
            [clojure.java.io :as io]
            [clojure.utils :as utils]
            [clojure.walk :as walk]
            [com.badlogic.gdx.backends.lwjgl3 :as lwjgl3])
  (:gen-class))

(def state (atom nil))

(defn start!
  [{:keys [os-settings
           config
           create
           dispose
           render
           resize]}]
  (clojure.gdx.utils/dispatch-on-os os-settings)
  (lwjgl3/start-application!
   {:create! (fn []
               (reset! state (utils/pipeline {} create)))
    :dispose! (fn []
                (swap! state dispose))
    :render! (fn []
               (swap! state utils/pipeline render)
               (stage/act!  (:ctx/stage @state))
               (stage/draw! (:ctx/stage @state)))
    :resize! (fn [width height]
               (swap! state resize width height))
    :pause! (fn [])
    :resume! (fn [])}
   config))

(defn- java-class? [s]
  (boolean (re-matches #".*\.[A-Z][A-Za-z0-9_]*" s)))

(defn- require-resolve-symbols [form] ; FIXME extends remove ?
  (if (symbol? form)
    (if (java-class? (str form))
      form
      (if (namespace form)
        (let [var (requiring-resolve form)]
          (assert var form)
          var)
        form))
    form))

(defn edn-resource [path]
  (->> path
       io/resource
       slurp
       (edn/read-string {:readers {'edn/resource edn-resource}})
       (walk/postwalk require-resolve-symbols)))

(defn -main [path]
  (start! (edn-resource path)))
