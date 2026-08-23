#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <X11/Xatom.h>

static int ignore_x_errors(Display *dpy, XErrorEvent *err) {
    (void)dpy;
    (void)err;
    return 0;
}

int main(int argc, char **argv) {
    if (argc < 2) {
        fprintf(stderr, "Usage: %s <path-to-image.ppm>\n", argv[0]);
        return 1;
    }

    const char *path = argv[1];
    FILE *fp = fopen(path, "rb");
    if (!fp) {
        perror("Failed to open PPM image");
        return 1;
    }

    char magic[3];
    if (fscanf(fp, "%2s", magic) != 1 || strcmp(magic, "P6") != 0) {
        fprintf(stderr, "Invalid PPM format (expected P6 binary)\n");
        fclose(fp);
        return 1;
    }

    int width = 0, height = 0, maxval = 0;
    if (fscanf(fp, "%d %d %d", &width, &height, &maxval) != 3 || width <= 0 || height <= 0 || maxval != 255) {
        fprintf(stderr, "Invalid PPM header\n");
        fclose(fp);
        return 1;
    }
    fgetc(fp); // consume whitespace / newline following header

    size_t rgb_size = (size_t)width * height * 3;
    unsigned char *rgb_data = (unsigned char *)malloc(rgb_size);
    if (!rgb_data) {
        fprintf(stderr, "Out of memory allocating RGB buffer\n");
        fclose(fp);
        return 1;
    }

    if (fread(rgb_data, 1, rgb_size, fp) != rgb_size) {
        fprintf(stderr, "Failed to read PPM pixel payload\n");
        free(rgb_data);
        fclose(fp);
        return 1;
    }
    fclose(fp);

    XSetErrorHandler(ignore_x_errors);

    Display *dpy = NULL;
    for (int i = 0; i < 30; i++) {
        dpy = XOpenDisplay(NULL);
        if (dpy) break;
        usleep(50000); // 50ms retry
    }

    if (!dpy) {
        fprintf(stderr, "Failed to connect to X display\n");
        free(rgb_data);
        return 1;
    }

    int screen = DefaultScreen(dpy);
    Window root = DefaultRootWindow(dpy);
    int depth = DefaultDepth(dpy, screen);
    Visual *visual = DefaultVisual(dpy, screen);

    size_t ximage_size = (size_t)width * height * 4;
    char *ximage_data = (char *)malloc(ximage_size);
    if (!ximage_data) {
        fprintf(stderr, "Out of memory allocating XImage buffer\n");
        free(rgb_data);
        XCloseDisplay(dpy);
        return 1;
    }

    // Convert 24-bit RGB to 32-bit BGRA (standard little-endian X11 ZPixmap format)
    for (int i = 0; i < width * height; i++) {
        unsigned char r = rgb_data[i * 3 + 0];
        unsigned char g = rgb_data[i * 3 + 1];
        unsigned char b = rgb_data[i * 3 + 2];
        ximage_data[i * 4 + 0] = b;
        ximage_data[i * 4 + 1] = g;
        ximage_data[i * 4 + 2] = r;
        ximage_data[i * 4 + 3] = (char)0xFF;
    }
    free(rgb_data);

    XImage *img = XCreateImage(dpy, visual, depth, ZPixmap, 0, ximage_data, width, height, 32, 0);
    if (!img) {
        fprintf(stderr, "Failed to create XImage\n");
        free(ximage_data);
        XCloseDisplay(dpy);
        return 1;
    }

    Pixmap pmap = XCreatePixmap(dpy, root, width, height, depth);
    GC gc = XCreateGC(dpy, pmap, 0, NULL);
    XPutImage(dpy, pmap, gc, img, 0, 0, 0, 0, width, height);

    // Set standard EWMH / Root pixmap atoms so Openbox and X11 managers recognize the root wallpaper
    Atom prop_root = XInternAtom(dpy, "_XROOTPMAP_ID", False);
    Atom prop_esetroot = XInternAtom(dpy, "ESETROOT_PMAP_ID", False);
    XChangeProperty(dpy, root, prop_root, XA_PIXMAP, 32, PropModeReplace, (unsigned char *)&pmap, 1);
    XChangeProperty(dpy, root, prop_esetroot, XA_PIXMAP, 32, PropModeReplace, (unsigned char *)&pmap, 1);

    // Set Root Window background and refresh
    XSetWindowBackgroundPixmap(dpy, root, pmap);
    XClearWindow(dpy, root);
    XFlush(dpy);

    XFreeGC(dpy, gc);
    XDestroyImage(img); // also frees ximage_data
    XCloseDisplay(dpy);
    return 0;
}
