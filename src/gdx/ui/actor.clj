(ns gdx.ui.actor
  (:import (com.badlogic.gdx.scenes.scene2d Actor
                                            Touchable)
           (com.badlogic.gdx.utils Align)
           (com.kotcrab.vis.ui.widget Tooltip
                                      VisLabel)
           (gdl.ui CtxStage)))

(defn get-stage-ctx [^Actor actor]
  (when-let [stage (.getStage actor)] ; for tooltip when actors are initialized w/o stage yet
    @(.ctx ^CtxStage stage)))

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

(defn add-tooltip! [actor tooltip-text]
  (let [text? (string? tooltip-text)
        label (VisLabel. (if text? tooltip-text ""))
        tooltip (proxy [Tooltip] []
                  ; hooking into getWidth because at
                  ; https://github.com/kotcrab/vis-blob/master/ui/src/main/java/com/kotcrab/vis/ui/widget/Tooltip.java#L271
                  ; when tooltip position gets calculated we setText (which calls pack) before that
                  ; so that the size is correct for the newly calculated text.
                  (getWidth []
                    (let [^Tooltip this this]
                      (when-not text?
                        (let [actor (.getTarget this)
                              ctx (get-stage-ctx actor)]
                          (when ctx ; ctx is only set later for update!/draw! ... not at starting of initialisation
                            (.setText this (str (tooltip-text ctx))))))
                      (proxy-super getWidth))))]
    (.setAlignment label Align/center)
    (.setTarget  tooltip actor)
    (.setContent tooltip label))
  actor)

(defn remove-tooltip! [actor]
  (Tooltip/removeTooltip actor))
