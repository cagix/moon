(ns clojure.gdx.scene2d.actor.opts
  (:require [clojure.scene2d.actor :as actor]))

(def fn-map
  {
   :actor/name
   (fn [actor name]
     (actor/set-name! actor name))

   :actor/user-object
   (fn [actor object]
     (actor/set-user-object! actor object))

   :actor/visible?
   (fn [actor bool]
     (actor/set-visible! actor (boolean bool)))

   :actor/position
   (fn [actor [x y]]
     (actor/set-position! actor x y))

   :actor/center-position
   (fn [actor [x y]]
     (actor/set-position! actor
                          (- x (/ (actor/get-width  actor) 2))
                          (- y (/ (actor/get-height actor) 2))))

   :actor/touchable
   (fn [actor touchable]
     (actor/set-touchable! actor touchable))

   :actor/listener
   (fn [actor listener]
     (actor/add-listener! actor listener))
   })

(defn set-actor-opts! [actor opts]
  (doseq [[k v] opts
          :let [f (get fn-map k)]
          :when f]
    (f actor v))
  actor)
