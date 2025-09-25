(ns cdq.application.create.stage.dev-menu.select-world
  (:require [cdq.ctx :as ctx]
            [gdl.scene2d.actor :as actor]
            [gdl.scene2d.stage :as stage]))

(def world-fns
  ["world_fns/vampire.edn"
   "world_fns/uf_caves.edn"
   "world_fns/modules.edn"])

(def menu
  {:label "Select World"
   :items (for [world-fn world-fns]
            {:label (str "Start " world-fn)
             :on-click (fn [actor ctx]
                         (stage/set-ctx! (actor/get-stage actor)
                                         (ctx/reset-game-state! ctx world-fn)))})})
