(ns com.badlogic.gdx.scenes.scene2d.actor
  (:require [com.badlogic.gdx.math.vector2 :as vector2]
            [com.badlogic.gdx.scenes.scene2d.touchable :as touchable]
            [gdl.scene2d.actor :as actor])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)))

(extend-type Actor
  actor/Actor
  (get-stage [actor]
    (.getStage actor))

  (get-x [actor]
    (.getX actor))

  (get-y [actor]
    (.getY actor))

  (get-name [actor]
    (.getName actor))

  (user-object [actor]
    (.getUserObject actor))

  (set-user-object! [actor object]
    (.setUserObject actor object))

  (visible? [actor]
    (.isVisible actor))

  (set-visible! [actor visible?]
    (.setVisible actor visible?))

  (set-touchable! [actor touchable]
    (.setTouchable actor (touchable/k->value touchable)))

  (remove! [actor]
    (.remove actor))

  (parent [actor]
    (.getParent actor))

  (stage->local-coordinates [actor position]
    (-> actor
        (.stageToLocalCoordinates (vector2/->java position))
        vector2/->clj))

  (hit [actor [x y]]
    (.hit actor x y true))

  (set-name! [actor name]
    (.setName actor name))

  (set-position! [actor x y]
    (.setPosition actor x y))

  (get-width [actor]
    (.getWidth actor))

  (get-height [actor]
    (.getHeight actor))

  (add-listener! [actor listener]
    (.addListener actor listener)))

(def opts-fn-map
  {:actor/name        actor/set-name!
   :actor/user-object actor/set-user-object!
   :actor/visible?    actor/set-visible!
   :actor/touchable   actor/set-touchable!
   :actor/listener    actor/add-listener!
   :actor/position (fn [a [x y]]
                     (actor/set-position! a x y))
   :actor/center-position (fn [a [x y]]
                            (actor/set-position! a
                                                 (- x (/ (actor/get-width  a) 2))
                                                 (- y (/ (actor/get-height a) 2))))})

(extend-type Actor
  actor/Opts
  (set-opts! [actor opts]
    (doseq [[k v] opts
            :let [f (get opts-fn-map k)]
            :when f]
      (f actor v))
    actor))

(defn create
  [{:keys [actor/act
           actor/draw]}]
  (proxy [Actor] []
    (act [delta]
      (act this delta)
      (proxy-super act delta))
    (draw [batch parent-alpha]
      (draw this batch parent-alpha))))
