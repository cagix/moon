(ns forge.app)

(declare screens
         ^:private current-screen-key)

(defn current-screen []
  (and (bound? #'current-screen-key)
       (current-screen-key screens)))

(defprotocol Screen
  (enter   [_])
  (exit    [_])
  (render  [_])
  (dispose [_]))

(defn change-screen
  "Calls `exit` on the current-screen and `enter` on the new screen."
  [new-k]
  (when-let [screen (current-screen)]
    (exit screen))
  (let [screen (new-k screens)]
    (assert screen (str "Cannot find screen with key: " new-k))
    (bind-root #'current-screen-key new-k)
    (enter screen)))

(defn render-current-screen []
  (render (current-screen)))
; hi
