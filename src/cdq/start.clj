(ns cdq.start
  (:require [clojure.edn :as edn]
            [clojure.gdx.utils.shared-library-loader :as shared-library-loader]
            [clojure.java.io :as io]
            [clojure.lwjgl.system.configuration :as lwjgl-configuration]
            [clojure.walk :as walk])
  (:gen-class))

(defn- bind-roots! [bindings]
  (doseq [[var-sym value] bindings]
    (clojure.lang.Var/.bindRoot (requiring-resolve var-sym)
                                (walk/postwalk
                                 (fn [x]
                                   (if (and (symbol? x) (namespace x))
                                     (try
                                      @(requiring-resolve x)
                                      (catch Exception _ x))
                                     x))
                                 value))))

(defn- set-mac-os-config! []
  (when (= (shared-library-loader/operating-system) :mac)
    (lwjgl-configuration/set-glfw-library-name! "glfw_async")))

(defn -main []
  (bind-roots! '[[cdq.draw-on-world-viewport.entities/render-layers [{:entity/mouseover? cdq.render-layers/draw-mouseover-highlighting
                                                                      :stunned cdq.render-layers/draw-stunned-state
                                                                      :player-item-on-cursor cdq.render-layers/draw-item-on-cursor-state}
                                                                     {:entity/clickable cdq.render-layers/draw-clickable-mouseover-text
                                                                      :entity/animation cdq.render-layers/call-render-image
                                                                      :entity/image cdq.render-layers/draw-centered-rotated-image
                                                                      :entity/line-render cdq.render-layers/draw-line-entity}
                                                                     {:npc-sleeping cdq.render-layers/draw-sleeping-state
                                                                      :entity/temp-modifier cdq.render-layers/draw-temp-modifiers
                                                                      :entity/string-effect cdq.render-layers/draw-text-over-entity}
                                                                     {:creature/stats cdq.render-layers/draw-stats
                                                                      :active-skill cdq.render-layers/draw-active-skill}]]])
  (set-mac-os-config!)
  (doseq [[f config] (-> "cdq.start.edn"
                         io/resource
                         slurp
                         edn/read-string)]
    ((requiring-resolve f) config)))
