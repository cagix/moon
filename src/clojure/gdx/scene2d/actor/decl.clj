(ns clojure.gdx.scene2d.actor.decl
  (:require [clojure.gdx.scene2d.actor :as actor]
            [clojure.gdx.scene2d.group :as group])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)))

(defmulti build :actor/type)

(def ^:private opts-fn-map
  {
   :actor/name actor/set-name!
   :actor/user-object actor/set-user-object!
   :actor/visible?  actor/set-visible!
   :actor/touchable actor/set-touchable!
   :actor/listener actor/add-listener!
   :actor/position
   (fn [actor [x y]]
     (actor/set-position! actor x y))
   :actor/center-position
   (fn [actor [x y]]
     (actor/set-position! actor
                          (- x (/ (actor/get-width  actor) 2))
                          (- y (/ (actor/get-height actor) 2))))
   })

(defn set-actor-opts! [actor opts]
  (doseq [[k v] opts
          :let [f (get opts-fn-map k)]
          :when f]
    (f actor v))
  actor)

(defn set-group-opts! [group opts]
  (run! (fn [actor-or-decl]
          (group/add! group (if (instance? Actor actor-or-decl)
                              actor-or-decl
                              (build actor-or-decl))))
        (:group/actors opts))
  (set-actor-opts! group opts))
