(ns cdq.stage
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.ui.action-bar]
            [cdq.ui.hp-mana-bar]
            [cdq.ui.inventory]
            [cdq.ui.player-state-draw]
            [cdq.ui.message]
            [cdq.ui.windows]
            [gdl.graphics.viewport :as viewport]
            [gdl.ui :as ui]
            [gdl.ui.actor :as actor])
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx.scenes.scene2d Stage)))

(defn add-actor! [stage actor]
  (Stage/.addActor stage actor))

(defn draw! [stage]
  (Stage/.draw stage))

(defn act! [stage]
  (Stage/.act stage))

(defn mouse-on-actor? [stage]
  (let [[x y] (viewport/mouse-position ctx/ui-viewport)]
    (Stage/.hit stage x y true)))

(defn root [stage]
  (Stage/.getRoot stage))

(defn set-item! [stage cell item]
  (cdq.ui.inventory/set-item! stage cell item))

(defn remove-item! [stage cell]
  (cdq.ui.inventory/remove-item! stage cell))

(defn selected-skill [stage]
  (cdq.ui.action-bar/selected-skill stage))

(defn add-skill! [stage skill]
  (cdq.ui.action-bar/add-skill! stage skill))

(defn remove-skill! [stage skill]
  (cdq.ui.action-bar/remove-skill! stage skill))

(defn show-message! [stage text]
  (cdq.ui.message/show! (root stage) text))

(defn create [{:keys [dev-menu]}]
  (let [stage (proxy [Stage ILookup] [(:java-object ctx/ui-viewport)
                                      (:java-object ctx/batch)]
                (valAt [id]
                  (ui/find-actor-with-id (Stage/.getRoot this) id)))]
    (run! #(Stage/.addActor stage %) [dev-menu
                                      (cdq.ui.action-bar/create)
                                      (cdq.ui.hp-mana-bar/create [(/ (:width ctx/ui-viewport) 2)
                                                                  80 ; action-bar-icon-size
                                                                  ])
                                      (cdq.ui.windows/create)
                                      (cdq.ui.player-state-draw/create)
                                      (cdq.ui.message/create)])
    stage))
