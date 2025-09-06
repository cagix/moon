(ns cdq.start
  (:require [clojure.edn :as edn]
            [clojure.gdx.utils.shared-library-loader :as shared-library-loader]
            [clojure.java.io :as io]
            [clojure.lwjgl.system.configuration :as lwjgl-configuration]
  (:gen-class))

(defn- bind-roots! [bindings]
  (doseq [[var-sym value] bindings]
    (clojure.lang.Var/.bindRoot (requiring-resolve var-sym) @(requiring-resolve value))))

(defn- set-mac-os-config! []
  (when (= (shared-library-loader/operating-system) :mac)
    (lwjgl-configuration/set-glfw-library-name! "glfw_async")))

(defn -main []
  (bind-roots! '[[cdq.draw-on-world-viewport.entities/render-layers cdq.entity.render/render-layers]])
  (set-mac-os-config!)
  (doseq [[f config] (-> "cdq.start.edn"
                         io/resource
                         slurp
                         edn/read-string)]
    ((requiring-resolve f) config)))
