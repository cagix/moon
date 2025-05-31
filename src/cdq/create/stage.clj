(ns cdq.create.stage
  (:require [cdq.g :as g]
            [cdq.ui.action-bar]
            [cdq.ui.dev-menu]
            [cdq.ui.entity-info]
            [cdq.ui.hp-mana-bar]
            [cdq.ui.inventory]
            [cdq.ui.message]
            [cdq.ui.player-state-draw]
            [cdq.ui.windows]
            [gdl.ui :as ui]
            [gdl.ui.menu :as menu])
  (:import (com.badlogic.gdx Gdx)))

(defn- create-actors [{:keys [ctx/ui-viewport]
                       :as ctx}]
  [(gdl.ui.menu/create (cdq.ui.dev-menu/create ctx))
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

(def ^:private -k :ctx/stage)

(defn do! [{:keys [ctx/ui-viewport
                   ctx/batch
                   ctx/config]
            :as ctx}]
  (ui/load! (:ui config))
  (extend (class ctx)
    g/Stage
    {:reset-actors! (fn [ctx]
                      (let [stage (-k ctx)]
                        (ui/clear! stage)
                        (run! #(ui/add! stage %) (create-actors ctx))))

     :add-actor! (fn [ctx actor]
                   (ui/add! (-k ctx) actor))

     :mouseover-actor (fn [ctx]
                        (ui/hit (-k ctx) (g/ui-mouse-position ctx)))

     :show-message! (fn [ctx message]
                      (-> (-k ctx)
                          ui/root
                          (ui/find-actor "player-message")
                          (cdq.ui.message/show! message)))

     ; no window movable type cursor appears here like in player idle
     ; inventory still working, other stuff not, because custom listener to keypresses ? use actor listeners?
     ; => input events handling
     ; hmmm interesting ... can disable @ item in cursor  / moving / etc.
     :show-modal! (fn [{:keys [ctx/ui-viewport] :as ctx}
                       {:keys [title text button-text on-click]}]
                    (let [stage (-k ctx)]
                      (assert (not (::modal stage)))
                      (ui/add! stage
                               (ui/window {:title title
                                           :rows [[(ui/label text)]
                                                  [(ui/text-button button-text
                                                                   (fn [_actor _ctx]
                                                                     (ui/remove! (::modal stage))
                                                                     (on-click)))]]
                                           :id ::modal
                                           :modal? true
                                           :center-position [(/ (:width ui-viewport) 2)
                                                             (* (:height ui-viewport) (/ 3 4))]
                                           :pack? true}))))

     :inventory-window-visible? (fn [ctx]
                                  (-> ctx -k :windows :inventory-window ui/visible?))

     :toggle-inventory-visible! (fn [ctx]
                                  (-> ctx -k :windows :inventory-window ui/toggle-visible!))
     })
  (let [stage (ui/stage (:java-object ui-viewport)
                        batch)]
    (.setInputProcessor Gdx/input stage)
    (assoc ctx -k stage)))
