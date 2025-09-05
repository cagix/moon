(ns clojure.gdx.scenes.scene2d.actor
  (:import (com.badlogic.gdx.scenes.scene2d Actor
                                            Touchable)
           (com.badlogic.gdx.scenes.scene2d.ui Button
                                               Label
                                               Window)
           (com.badlogic.gdx.math Vector2)
           (com.kotcrab.vis.ui.widget VisWindow)))

(defn get-stage [^Actor actor]
  (.getStage actor))

(defn get-x [^Actor actor]
  (.getX actor))

(defn get-y [^Actor actor]
  (.getY actor))

(defn get-name [^Actor actor]
  (.getName actor))

(defn user-object [^Actor actor]
  (.getUserObject actor))

(defn set-user-object! [^Actor actor object]
  (.setUserObject actor object))

(defn visible? [^Actor actor]
  (.isVisible actor))

(defn set-visible! [^Actor actor visible?]
  (.setVisible actor visible?))

(defn set-touchable! [^Actor actor touchable]
  (.setTouchable actor (case touchable
                         :disabled Touchable/disabled)))

(defn remove! [^Actor actor]
  (.remove actor))

(defn parent [^Actor actor]
  (.getParent actor))

(defn hit [^Actor actor [x y]]
  (let [v (.stageToLocalCoordinates actor (Vector2. x y))]
    (.hit actor (.x v) (.y v) true)))

(defn set-opts!
  [^Actor actor
   {:keys [id
           name
           user-object
           visible?
           center-position
           position] :as opts}]
  (when id
    (set-user-object! actor id))
  (when name
    (.setName actor name))
  (when user-object
    (set-user-object! actor user-object))
  (when (contains? opts :visible?)
    (set-visible! actor visible?))
  (when-let [[x y] center-position]
    (.setPosition actor
                  (- x (/ (.getWidth  actor) 2))
                  (- y (/ (.getHeight actor) 2))))
  (when-let [[x y] position]
    (.setPosition actor x y))
  (when-let [touchable (:actor/touchable opts)]
    (set-touchable! actor touchable))
  actor)

(defmulti construct :actor/type)

(defn construct? ^Actor [actor-declaration]
  (try
   (cond
    (instance? Actor actor-declaration) actor-declaration
    (map? actor-declaration) (construct actor-declaration)
    (nil? actor-declaration) nil
    :else (throw (ex-info "Cannot find constructor"
                          {:instance-actor? (instance? Actor actor-declaration)
                           :map? (map? actor-declaration)})))
   (catch Throwable t
     (throw (ex-info "Cannot create-actor"
                     {:actor-declaration actor-declaration}
                     t)))))

(defn- button-class? [actor]
  (some #(= Button %) (supers (class actor))))

(defn button?
  "Returns true if the actor or its parent is a button."
  [^Actor actor]
  (or (button-class? actor)
      (and (parent actor)
           (button-class? (parent actor)))))

; TODO buggy FIXME
(defn window-title-bar?
  "Returns true if the actor is a window title bar."
  [^Actor actor]
  (when (instance? Label actor)
    (when-let [p (parent actor)]
      (when-let [p (parent p)]
        (and (instance? VisWindow actor)
             (= (.getTitleLabel ^Window p) actor))))))

(defn find-ancestor-window ^Window [actor]
  (if-let [p (parent actor)]
    (if (instance? Window p)
      p
      (find-ancestor-window p))
    (throw (Error. (str "Actor has no parent window " actor)))))

(defn pack-ancestor-window! [actor]
  (.pack (find-ancestor-window actor)))

(defn toggle-visible! [actor]
  (set-visible! actor (not (visible? actor))))
