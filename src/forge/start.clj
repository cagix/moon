(ns forge.start
  (:require [clojure.awt :as awt]
            [clojure.edn :as edn]
            [clojure.gdx.backends.lwjgl3 :as lwjgl3]
            [clojure.gdx.utils.shared-library-loader :as shared-library-loader]
            [clojure.java.io :as io]
            [clojure.lwjgl :as lwjgl]
            [clojure.utils :refer [install install-component]]
            [forge.app :as component]))

(defn install-app-components [components]
  (install "forge"
           {:optional [#'component/create
                       #'component/destroy
                       #'component/render
                       #'component/resize]}
           (map vector components)))

(require '[forge.entity :as entity])

(def entity
  {:optional [#'entity/->v
              #'entity/create
              #'entity/destroy
              #'entity/tick
              #'entity/render-below
              #'entity/render-default
              #'entity/render-above
              #'entity/render-info]})

(defn install-entity-components [components]
  (install "forge"
           entity
           (map vector components)))

(require '[forge.effect :as effect])

(defn install-effect-components [components]
  (install "forge"
           {:required [#'effect/applicable?
                       #'effect/handle]
            :optional [#'effect/useful?
                       #'effect/render-effect]}
           (map vector components)))

(require '[forge.entity.state :as state])

(def entity-state
  (merge-with concat
              entity
              {:optional [#'state/enter
                          #'state/exit
                          #'state/cursor
                          #'state/pause-game?
                          #'state/manual-tick
                          #'state/clicked-inventory-cell
                          #'state/clicked-skillmenu-skill
                          #'state/draw-gui-view]}))

(doseq [[ns-sym k] '[[forge.entity.state.active-skill :active-skill]
                     [forge.entity.state.npc-dead :npc-dead]
                     [forge.entity.state.npc-idle :npc-idle]
                     [forge.entity.state.npc-moving :npc-moving]
                     [forge.entity.state.npc-sleeping :npc-sleeping]
                     [forge.entity.state.player-dead :player-dead]
                     [forge.entity.state.player-idle :player-idle]
                     [forge.entity.state.player-item-on-cursor :player-item-on-cursor]
                     [forge.entity.state.player-moving :player-moving]
                     [forge.entity.state.stunned :stunned]]]
  (install-component entity-state ns-sym k))

(defn -main []
  (let [{:keys [components install] :as config} (-> "app.edn" io/resource slurp edn/read-string)]
    (install-entity-components (:entity install))
    (install-effect-components (:effect install))
    (install-app-components    (:app    install))
    (run! require (:requires config))
    (awt/set-dock-icon (:dock-icon config))
    (when shared-library-loader/mac?
      (lwjgl/configure-glfw-for-mac))
    (lwjgl3/app (reify lwjgl3/Listener
                  (create  [_]     (run! component/create          components))
                  (dispose [_]     (run! component/destroy         components))
                  (render  [_]     (run! component/render          components))
                  (resize  [_ w h] (run! #(component/resize % w h) components)))
                (lwjgl3/config (:lwjgl3 config)))))
