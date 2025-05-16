(ns clojure.ui
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx.scenes.scene2d Actor Group)))

(defn find-actor-with-id [^Group group id]
  (let [actors (.getChildren group)
        ids (keep Actor/.getUserObject actors)]
    (assert (or (empty? ids)
                (apply distinct? ids)) ; TODO could check @ add
            (str "Actor ids are not distinct: " (vec ids)))
    (first (filter #(= id (Actor/.getUserObject %)) actors))))

(defmacro proxy-ILookup
  "For actors inheriting from Group, implements `clojure.lang.ILookup` (`get`)
  via [find-actor-with-id]."
  [class args]
  `(proxy [~class ILookup] ~args
     (valAt
       ([id#]
        (find-actor-with-id ~'this id#))
       ([id# not-found#]
        (or (find-actor-with-id ~'this id#) not-found#)))))
