(ns cdq.stage
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.ui]
            [cdq.ui.action-bar]
            [cdq.ui.hp-mana-bar]
            [cdq.ui.inventory]
            [cdq.ui.player-state-draw]
            [cdq.ui.message]
            [cdq.dev-menu-config]
            [cdq.ui.windows]
            [gdl.graphics.viewport :as viewport]
            [gdl.ui :as ui]
            [gdl.ui.actor :as actor])
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx.scenes.scene2d Stage)))

(defn- create-actors []
  [(cdq.ui/menu (cdq.dev-menu-config/create))
   (cdq.ui.action-bar/create)
   (cdq.ui.hp-mana-bar/create [(/ (:width ctx/ui-viewport) 2)
                               80 ; action-bar-icon-size
                               ])
   (cdq.ui.windows/create)
   (cdq.ui.player-state-draw/create)
   (cdq.ui.message/create)])

(defprotocol PStage
  (add-actor! [_ actor])
  (draw! [_])
  (act! [_])
  (mouse-on-actor? [_])
  (root [_])
  (set-item! [stage cell item])
  (remove-item! [stage cell])
  (selected-skill [stage])
  (add-skill! [stage skill])
  (remove-skill! [stage skill])
  (show-message! [stage text]))

(defn create! []
  (let [stage (Stage. (:java-object ctx/ui-viewport)
                      (:java-object ctx/batch))]
    (run! #(.addActor stage %) (create-actors))
    (reify
      ILookup
      (valAt [_ id]
        (ui/find-actor-with-id (.getRoot stage) id))

      (valAt [_ id not-found]
        (or (ui/find-actor-with-id (.getRoot stage) id)
            not-found))

      PStage
      (add-actor! [_ actor]
        (.addActor stage actor))

      (draw! [_]
        (.draw stage))

      (act! [_]
        (.act stage))

      (mouse-on-actor? [_]
        (let [[x y] (viewport/mouse-position ctx/ui-viewport)]
          (.hit stage x y true)))

      (root [_]
        (.getRoot stage))

      (set-item! [stage cell item]
        (cdq.ui.inventory/set-item! stage cell item))

      (remove-item! [stage cell]
        (cdq.ui.inventory/remove-item! stage cell))

      (selected-skill [stage]
        (cdq.ui.action-bar/selected-skill stage))

      (add-skill! [stage skill]
        (cdq.ui.action-bar/add-skill! stage skill))

      (remove-skill! [stage skill]
        (cdq.ui.action-bar/remove-skill! stage skill))

      (show-message! [stage text]
        (cdq.ui.message/show! (root stage) text)))))
