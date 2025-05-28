(ns cdq.create.stage
  (:require [cdq.g :as g]
            [cdq.ui.dev-menu]
            [cdq.ui.action-bar]
            [cdq.ui.hp-mana-bar]
            [cdq.ui.windows]
            [cdq.ui.entity-info]
            [cdq.ui.inventory]
            [cdq.ui.player-state-draw]
            [cdq.ui.message]
            [gdl.ui :as ui]
            [gdl.viewport :as viewport])
  (:import (com.badlogic.gdx Gdx)))

(defn add-stage! [ctx]
  (let [stage (ui/stage (:java-object (:ctx/ui-viewport ctx))
                        (:batch (:ctx/graphics ctx)))]
    (.setInputProcessor Gdx/input stage)
    (assoc ctx :ctx/stage stage)))

(defn- create-actors [{:keys [ctx/ui-viewport]
                       :as ctx}]
  [(cdq.ui.dev-menu/create ctx)
   (cdq.ui.action-bar/create :id :action-bar)
   (cdq.ui.hp-mana-bar/create [(/ (:width ui-viewport) 2)
                               80 ; action-bar-icon-size
                               ]
                              ctx)
   (cdq.ui.windows/create :id :windows
                          :actors [(cdq.ui.entity-info/create [(:width ui-viewport) 0])
                                   (cdq.ui.inventory/create ctx
                                                            :id :inventory-window
                                                            :position [(:width ui-viewport)
                                                                       (:height ui-viewport)])])
   (cdq.ui.player-state-draw/create)
   (cdq.ui.message/create :name "player-message")])

(extend-type gdl.application.Context
  g/Stage
  (find-actor-by-name [{:keys [ctx/stage]} name]
    (-> stage
        ui/root
        (ui/find-actor name))) ; <- find-actor protocol & for stage use ui/root

  (mouseover-actor [{:keys [ctx/ui-viewport
                            ctx/stage]}]
    (ui/hit stage (viewport/mouse-position ui-viewport)))

  (reset-actors! [{:keys [ctx/stage] :as ctx}]
    (ui/clear! stage)
    (run! #(ui/add! stage %) (create-actors ctx))))
