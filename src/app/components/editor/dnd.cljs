(ns app.components.editor.dnd
  (:require
   [uix.core :as uix]))

(defn use-draggable [{:keys [meta on-drag-move on-drag-end]}]
  (let [[drag-opts set-drag-opts!] (uix/use-state nil)
        on-pointer-down (uix/use-callback
                         (fn [e]
                           (.preventDefault e)
                           (set-drag-opts! {:meta meta
                                            :start-coords {:x (.-clientX e)
                                                           :y (.-clientY e)}
                                            :element (.-target e)}))
                         [meta])
        !dragging-opts (uix/use-ref)]
    (uix/use-effect
     (fn []
       (when-let [{:keys [start-coords]} drag-opts]
         (letfn [(on-pointer-move [e]
                   (let [delta {:x (- (:x start-coords) (.-clientX e))
                                :y (- (:y start-coords) (.-clientY e))}
                         opts (assoc drag-opts :delta delta)]
                     (on-drag-move opts)
                     (reset! !dragging-opts opts)))
                 (unsub []
                   (.removeEventListener js/window "pointermove" on-pointer-move)
                   (.removeEventListener js/window "pointerup" unsub)
                   (when @!dragging-opts
                     (on-drag-end @!dragging-opts)
                     (reset! !dragging-opts nil)
                     (set-drag-opts! nil)))]
           (.addEventListener js/window "pointermove" on-pointer-move)
           (.addEventListener js/window "pointerup" unsub {:once true})
           unsub)))
     [meta drag-opts on-drag-move on-drag-end])
    {:on-pointer-down on-pointer-down}))
