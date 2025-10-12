(ns cdq.ui.action-bar
  (:require [clojure.gdx.scene2d.actor :as actor]
            [clojure.gdx.scene2d.group :as group]
            [clojure.gdx.scene2d.ui.button-group :as button-group]
            [cdq.ui.build.horizontal-group :as horiz-group]
            [cdq.ui.build.table :as table]
            [clojure.scene2d.vis-ui.image-button :as image-button]))

; An UI widget has a initial constructor (create)
; plus one which it inherits (which calls other inherited constructors)
; so the defmulti was quite good but you mixed it up with the 'facade'
; so first fix the facade
; imports
; the 'opts' are loaded dynamically
; there is no build? but _only_ build via actor type then later (or [k opts] ? )

; * first release 'clojure.vis-ui' & 'clojure.gdx.scene2d' facades separately
; (separate clojure.gdx libs)

(comment
 [:ui/table
  {:table/rows [[{:cell/actor (horiz-group/create
                               {:pad 2
                                :space 2
                                :actor/name "cdq.ui.action-bar.horizontal-group"
                                :actor/user-object (button-group/create
                                                    {:min-check-count 0
                                                     :max-check-count 1})})
                  :cell/expand? true
                  :cell/bottom? true}]]
   :actor/name "cdq.ui.action-bar"
   :table/cell-defaults {:pad 2}
   :widget-group/fill-parent? true}]
 )

(defn create []
  (table/create
   {:rows [[{:actor (horiz-group/create
                     {:pad 2
                      :space 2
                      :actor/name "cdq.ui.action-bar.horizontal-group"
                      :actor/user-object (button-group/create
                                          {:min-check-count 0
                                           :max-check-count 1})})
             :expand? true
             :bottom? true}]]
    :actor/name "cdq.ui.action-bar"
    :cell-defaults {:pad 2}
    :fill-parent? true}))

(defn- get-data [action-bar]
  (let [group (group/find-actor action-bar "cdq.ui.action-bar.horizontal-group")]
    {:horizontal-group group
     :button-group (actor/user-object group)}))

(defn selected-skill [action-bar]
  (when-let [skill-button (button-group/checked (:button-group (get-data action-bar)))]
    (actor/user-object skill-button)))

(defn add-skill!
  [action-bar
   {:keys [skill-id
           texture-region
           tooltip-text]}]
  (let [{:keys [horizontal-group button-group]} (get-data action-bar)
        button (image-button/create
                {:actor/user-object skill-id
                 :drawable/texture-region texture-region
                 :drawable/scale 2
                 :tooltip tooltip-text})]
    (group/add-actor! horizontal-group button)
    (button-group/add! button-group button)
    nil))

(defn remove-skill! [action-bar skill-id]
  (let [{:keys [horizontal-group button-group]} (get-data action-bar)
        button (get horizontal-group skill-id)]
    (actor/remove! button)
    (button-group/remove! button-group button)
    nil))
