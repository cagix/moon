(ns cdq.create.ui.entity-info
  (:require [cdq.ctx :as ctx]
            [cdq.stage :as stage]
            [gdl.scene2d :as scene2d]
            [gdl.scene2d.group :as group]
            [gdl.scene2d.ui.label :as label]
            [gdl.scene2d.ui.widget-group :as widget-group]))

(comment

 ; items then have 2x pretty-name
 #_(.setText (.getTitleLabel window)
             (info-text [:property/pretty-name (:property/pretty-name entity)])
             "Entity Info")
 )

(def disallowed-keys [:entity/skills
                      #_:entity/fsm
                      :entity/faction
                      :active-skill])

; TODO details how the text looks move to info
; only for :
; * skill
; * entity -> all sub-types
; * item
; => can test separately !?

(defn- ->label-text [entity ctx]
  ; don't use select-keys as it loses Entity record type
  (ctx/info-text ctx (apply dissoc entity disallowed-keys)))

(defn create [{:keys [ctx/stage]}]
  (let [y-position 0
        position [(stage/viewport-width stage)
                  y-position]
        label (scene2d/build {:actor/type :actor.type/label
                              :label/text ""})
        window (scene2d/build {:actor/type :actor.type/window
                               :title "Info"
                               :actor/name "cdq.ui.windows.entity-info"
                               :actor/visible? false
                               :actor/position position
                               :rows [[{:actor label :expand? true}]]})]
    ; do not change window size ... -> no need to invalidate layout, set the whole stage up again
    ; => fix size somehow.
    (group/add! window (scene2d/build
                        {:actor/type :actor.type/actor
                         :act (fn [_this _delta {:keys [ctx/world]
                                                 :as ctx}]
                                (label/set-text! label (str (if-let [eid (:world/mouseover-eid world)]
                                                              (->label-text @eid ctx)
                                                              "")))
                                (widget-group/pack! window))}))
    window))
