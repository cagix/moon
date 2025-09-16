(ns clojure.gdx.backends.lwjgl
  (:require [cdq.application :as application]
            [clojure.edn :as edn]
            [clojure.gdx.utils]
            [clojure.scene2d.stage :as stage]
            [clojure.java.io :as io]
            [clojure.utils :as utils]
            [clojure.walk :as walk]
            [com.badlogic.gdx.backends.lwjgl3 :as lwjgl3])
  (:gen-class))

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
               (reset! application/state (utils/pipeline {} create)))
    :dispose! (fn []
                (swap! application/state dispose))
    :render! (fn []
               (swap! application/state utils/pipeline render)
               (stage/act!  (:ctx/stage @application/state))
               (stage/draw! (:ctx/stage @application/state)))
    :resize! (fn [width height]
               (swap! application/state resize width height))
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
