import { Component, Input } from "@angular/core";

@Component({
    selector: "lib-library",
    standalone: true,
    imports: [],
    templateUrl: "./library.component.html",
    styleUrl: "./library.component.scss"
})
export class LibraryComponent {
    @Input() reactValue = "";

    count = 0;
    inputValue = "";
    onKey(event: any) {
        this.inputValue = event.target.value;
        document.dispatchEvent(
            new CustomEvent("angular-input-event", {
                detail: event.target.value
            })
        );
    }

    increment() {
        ++this.count;
        document.dispatchEvent(
            new CustomEvent("angular-click-event", {
                detail: this.count
            })
        );
    }
}
