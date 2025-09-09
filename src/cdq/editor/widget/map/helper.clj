(ns cdq.editor.widget.map.helper
  (:require [cdq.editor-window :as editor-window]
            [cdq.utils :as utils]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.group :as group]))

(defn k->label-text [k]
  (name k) ;(str "[GRAY]:" (namespace k) "[]/" (name k))
  )

(defn- handle-click [k table]
  (fn [_actor ctx]
    (actor/remove! (utils/find-first (fn [actor]
                                       (and (actor/user-object actor)
                                            (= k ((actor/user-object actor) 0))))
                                     (group/children table)))
    (editor-window/rebuild! ctx)))

(defn label-cell
  [{:keys [display-remove-component-button? k table label-text]}]
  {:actor {:actor/type :actor.type/table
           :cell-defaults {:pad 2}
           :rows [[{:actor (when display-remove-component-button?
                             {:actor/type :actor.type/text-button
                              :text "-"
                              :on-clicked (handle-click k table)})
                    :left? true}
                   {:actor {:actor/type :actor.type/label
                            :label/text label-text}}]]}
   :right? true})
