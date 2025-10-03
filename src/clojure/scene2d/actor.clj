(ns clojure.scene2d.actor
  (:require [gdl.scene2d.actor :as actor]
            [com.badlogic.gdx.scenes.scene2d.ctx :as ctx]
            [com.badlogic.gdx.scenes.scene2d.stage :as stage]))

(def opts-fn-map
  {:actor/name (fn [a name] (actor/set-name! a name))
   :actor/user-object (fn [a object] (actor/set-user-object! a object))
   :actor/visible?  (fn [a visible?] (actor/set-visible! a visible?))
   :actor/touchable (fn [a touchable] (actor/set-touchable! a touchable))
   :actor/listener (fn [a listener] (actor/add-listener! a listener))
   :actor/position (fn [a [x y]]
                     (actor/set-position! a x y))
   :actor/center-position (fn [a [x y]]
                            (actor/set-position! a
                                                 (- x (/ (actor/get-width  a) 2))
                                                 (- y (/ (actor/get-height a) 2))))})

(defn set-opts! [actor opts]
  (doseq [[k v] opts
          :let [f (get opts-fn-map k)]
          :when f]
    (f actor v))
  actor)

(defn- get-ctx [actor]
  (when-let [stage (actor/get-stage actor)]
    (stage/get-ctx stage)))

(defn act! [actor delta f]
  (when-let [ctx (get-ctx actor)]
    (f actor delta ctx)))

(defn draw! [actor f]
  (when-let [ctx (get-ctx actor)]
    (ctx/draw! ctx (f actor ctx))))
