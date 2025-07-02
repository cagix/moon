(ns cdq.ui.windows.entity-info
  (:require [cdq.ctx :as ctx]
            [gdl.ui.group :as group]
            [gdx.ui :as ui]))

(comment

 ; items then have 2x pretty-name
 #_(.setText (.getTitleLabel window)
             (ctx/info-text [:property/pretty-name (:property/pretty-name entity)])
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

(defn- create* [position]
  (let [label (ui/label {:label/text ""})
        window (ui/window {:title "Info"
                           :id :entity-info-window
                           :visible? false
                           :position position
                           :rows [[{:actor label :expand? true}]]})]
    ; do not change window size ... -> no need to invalidate layout, set the whole stage up again
    ; => fix size somehow.
    (group/add! window {:actor/type :actor.type/actor
                        :act (fn [_this _delta {:keys [ctx/world]
                                                :as ctx}]
                               (.setText label (str (if-let [eid (:world/mouseover-eid world)]
                                                      (->label-text @eid ctx)
                                                      "")))
                               (.pack window))})
    window))

(defn create [{:keys [ctx/graphics]}
              {:keys [y]}]
  (create* [(:viewport/width (:ui-viewport graphics))
            y]))
