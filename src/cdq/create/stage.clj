(ns cdq.create.stage
  (:require [cdq.graphics :as g]
            [cdq.input :as input]
            [cdq.stage]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.dev-menu]
            [cdq.ui.entity-info]
            [cdq.ui.error-window :as error-window]
            [cdq.ui.hp-mana-bar]
            [cdq.ui.inventory :as inventory-window]
            [cdq.ui.message]
            [cdq.ui.player-state-draw]
            [cdq.ui.windows]
            [gdl.ui :as ui]
            [gdl.ui.stage]
            [gdl.ui.menu :as menu]))

(defn- create-actors [{:keys [ctx/ui-viewport]
                       :as ctx}]
  [(gdl.ui.menu/create (cdq.ui.dev-menu/create ctx))
   (action-bar/create :id :action-bar)
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

; TODO can we make it a protocol ?
; gdl.ui.stage ?
; or how is it accessed?

; => as ILookup -> possible ?

; * act!
; * draw!
; * add!
; * hit
; * find by name string

; => then we can remove stage from gdl.ui ????

; also outdated context -> pass directly atom state?
; swap! at each render?

(defn do! [{:keys [ctx/ui-viewport
                   ctx/batch
                   ctx/config]
            :as ctx}]
  (ui/load! (:ui config))
  (extend (class ctx)
    cdq.stage/Stage
    {:render-stage! (fn [ctx]
                      (gdl.ui.stage/render! (-k ctx) ctx))

     :reset-actors! (fn [ctx]
                      (let [stage (-k ctx)]
                        (gdl.ui.stage/clear! stage)
                        (run! #(gdl.ui.stage/add! stage %) (create-actors ctx))))

     :add-actor! (fn [ctx actor]
                   (gdl.ui.stage/add! (-k ctx) actor))

     :mouseover-actor (fn [ctx]
                        (gdl.ui.stage/hit (-k ctx) (g/ui-mouse-position ctx)))

     :show-message! (fn [ctx message]
                      (-> (-k ctx)
                          (gdl.ui.stage/find-actor "player-message")
                          (cdq.ui.message/show! message)))

     ; no window movable type cursor appears here like in player idle
     ; inventory still working, other stuff not, because custom listener to keypresses ? use actor listeners?
     ; => input events handling
     ; hmmm interesting ... can disable @ item in cursor  / moving / etc.
     :show-modal! (fn [{:keys [ctx/ui-viewport] :as ctx}
                       {:keys [title text button-text on-click]}]
                    (let [stage (-k ctx)]
                      (assert (not (::modal stage)))
                      (gdl.ui.stage/add! stage
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

     :add-skill! (fn [ctx skill]
                   (-> ctx -k :action-bar (action-bar/add-skill! skill)))

     :remove-skill! (fn [ctx skill]
                      (-> ctx -k :action-bar (action-bar/remove-skill! skill)))

     :set-item! (fn [ctx inventory-cell item]
                  (-> ctx -k :windows :inventory-window (inventory-window/set-item! inventory-cell item)))

     :remove-item! (fn [ctx inventory-cell]
                     (-> ctx -k :windows :inventory-window (inventory-window/remove-item! inventory-cell)))

     :open-error-window! (fn [ctx throwable]
                           (gdl.ui.stage/add! (-k ctx) (error-window/create throwable)))

     :selected-skill (fn [ctx]
                       (action-bar/selected-skill (:action-bar (-k ctx))))

     })
  (let [stage (ui/stage (:java-object ui-viewport)
                        batch)]
    (input/set-processor! ctx stage)
    (assoc ctx -k (reify
                    ; TODO is disposable but not sure if needed as we handle batch ourself.
                    clojure.lang.ILookup
                    (valAt [_ key]
                      (key stage))

                    gdl.ui.stage/Stage
                    (render! [_ ctx]
                      (ui/act! stage ctx)
                      (ui/draw! stage ctx)
                      ctx)

                    (add! [_ actor] ; -> use gdl.ui/add! ?
                      (ui/add! stage actor))

                    (clear! [_]
                      (ui/clear! stage))

                    (hit [_ position]
                      (ui/hit stage position))

                    (find-actor [_ actor-name]
                      (-> stage
                          ui/root
                          (ui/find-actor actor-name)))))))
